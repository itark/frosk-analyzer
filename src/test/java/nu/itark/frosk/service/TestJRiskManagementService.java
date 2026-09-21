package nu.itark.frosk.service;

import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context — RiskManagementService is a pure
 * constructor-injected POJO service, same style as TestJRegimeForecastService.
 */
public class TestJRiskManagementService {

    private static final BigDecimal DAILY_MAX_LOSS_USD = BigDecimal.valueOf(-500);
    private static final BigDecimal MAX_POSITION_PCT = BigDecimal.valueOf(0.10);
    private static final int MAX_OPEN_POSITIONS = 5;

    private RiskManagementService newService(boolean enabled, BigDecimal realizedTodayUsd, long openPositionCount) {
        KrakenFuturesPaperOrderRepository repo = mock(KrakenFuturesPaperOrderRepository.class);
        when(repo.sumRealizedPnlUsdSince(any())).thenReturn(realizedTodayUsd);
        when(repo.countByStatus("OPEN")).thenReturn(openPositionCount);

        RiskManagementService service = new RiskManagementService(repo);
        ReflectionTestUtils.setField(service, "enabled", enabled);
        ReflectionTestUtils.setField(service, "dailyMaxLossUsd", DAILY_MAX_LOSS_USD);
        ReflectionTestUtils.setField(service, "maxPositionPct", MAX_POSITION_PCT);
        ReflectionTestUtils.setField(service, "maxOpenPositions", MAX_OPEN_POSITIONS);
        return service;
    }

    @Test
    void withinAllLimits_isAllowed() {
        RiskManagementService service = newService(true, BigDecimal.ZERO, 1);

        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(500), BigDecimal.valueOf(10000));

        assertTrue(result.allowed());
        assertNull(result.reason());
    }

    @Test
    void dailyLossBreached_tripsCircuitBreaker_andBlocksEntry() {
        // -600 realized today is worse than the -500 limit.
        RiskManagementService service = newService(true, BigDecimal.valueOf(-600), 1);

        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(100), BigDecimal.valueOf(10000));

        assertFalse(result.allowed());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("halted"), "reason should explain the circuit breaker: " + result.reason());
        assertTrue(service.isTradingHalted());
    }

    @Test
    void positionTooLarge_isBlocked() {
        RiskManagementService service = newService(true, BigDecimal.ZERO, 1);
        // 10% of 10,000 = 1,000 max; propose 1,500.
        BigDecimal accountValue = BigDecimal.valueOf(10000);
        BigDecimal proposed = BigDecimal.valueOf(1500);

        RiskCheckResult result = service.checkEntry(proposed, accountValue);

        assertFalse(result.allowed());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("Position size"), "reason should explain the size breach: " + result.reason());
    }

    @Test
    void positionWithinLimit_isAllowed() {
        RiskManagementService service = newService(true, BigDecimal.ZERO, 1);
        // Exactly 10% of 10,000 = 1,000 — at the limit, not over it.
        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(1000), BigDecimal.valueOf(10000));

        assertTrue(result.allowed());
    }

    @Test
    void maxOpenPositionsReached_isBlocked() {
        RiskManagementService service = newService(true, BigDecimal.ZERO, MAX_OPEN_POSITIONS);

        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(100), BigDecimal.valueOf(10000));

        assertFalse(result.allowed());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("open positions"), "reason should explain the position-count breach: " + result.reason());
    }

    @Test
    void belowMaxOpenPositions_isAllowed() {
        RiskManagementService service = newService(true, BigDecimal.ZERO, MAX_OPEN_POSITIONS - 1);

        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(100), BigDecimal.valueOf(10000));

        assertTrue(result.allowed());
    }

    @Test
    void disabled_alwaysAllowed_noRepositoryQueries() {
        KrakenFuturesPaperOrderRepository repo = mock(KrakenFuturesPaperOrderRepository.class);
        RiskManagementService service = new RiskManagementService(repo);
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "dailyMaxLossUsd", DAILY_MAX_LOSS_USD);
        ReflectionTestUtils.setField(service, "maxPositionPct", MAX_POSITION_PCT);
        ReflectionTestUtils.setField(service, "maxOpenPositions", MAX_OPEN_POSITIONS);

        // Even a wildly out-of-bounds proposal must be allowed when disabled.
        RiskCheckResult result = service.checkEntry(BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(1));

        assertTrue(result.allowed());
        assertNull(result.reason());
        assertFalse(service.isEnabled());
        verifyNoInteractions(repo);
    }
}
