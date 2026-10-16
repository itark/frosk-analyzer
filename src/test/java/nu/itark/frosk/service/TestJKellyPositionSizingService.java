package nu.itark.frosk.service;

import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (Mockito, no Spring context) for {@link KellyPositionSizingService}.
 * Trades are given as returns on their entry notional (entry 100 EUR, pnl = return × 100).
 */
@ExtendWith(MockitoExtension.class)
public class TestJKellyPositionSizingService {

    private static final String STRATEGY = "CryptoVWAPReversionIntradayStrategy";

    @Mock
    private CryptoPaperOrderRepository orderRepository;

    private KellyPositionSizingService service;

    @BeforeEach
    void setUp() {
        service = new KellyPositionSizingService();
        ReflectionTestUtils.setField(service, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minTrades", 20);
    }

    private static CryptoPaperOrder sell(double ret, BigDecimal entry) {
        CryptoPaperOrder o = new CryptoPaperOrder();
        o.setSide("SELL");
        o.setStrategyName(STRATEGY);
        o.setEntryEurAmount(entry);
        o.setRealizedPnlEur(BigDecimal.valueOf(ret * 100));
        return o;
    }

    private void history(int wins, double winRet, int losses, double lossRet) {
        List<CryptoPaperOrder> rows = new ArrayList<>();
        for (int i = 0; i < wins; i++) rows.add(sell(winRet, BigDecimal.valueOf(100)));
        for (int i = 0; i < losses; i++) rows.add(sell(-lossRet, BigDecimal.valueOf(100)));
        when(orderRepository.findTop100ByStrategyNameAndSideOrderByCreatedAtDesc(STRATEGY, "SELL")).thenReturn(rows);
    }

    @Test
    void quarterKellyFromWinRateAndPayoff() {
        // W = 0.6, R = 0.02 / 0.01 = 2 → f* = 0.6 - 0.4/2 = 0.4 → 0.25 × 0.4 = 0.10 → capped at 5 %
        history(60, 0.02, 40, 0.01);
        assertEquals(0.05, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
    }

    @Test
    void inBetweenFractionIsNotClamped() {
        // W = 0.6, R = 0.015 / 0.015 = 1 → f* = 0.2 → 0.25 × 0.2 = 0.05; W = 0.55 → f* = 0.1 → 0.025
        history(55, 0.015, 45, 0.015);
        assertEquals(0.025, service.kellyFraction(STRATEGY).doubleValue(), 1e-9);
    }

    @Test
    void negativeEdgeFloorsAtMinimum() {
        // Recent VWAP-like week: W = 0.5, avg win 1.5 %, avg loss 2.7 % → f* < 0
        history(50, 0.015, 50, 0.027);
        assertEquals(0.005, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
    }

    @Test
    void fallbackBelowMinTrades() {
        history(15, 0.02, 4, 0.01);
        assertEquals(0.02, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
    }

    @Test
    void rowsWithoutEntryNotionalDoNotCount() {
        List<CryptoPaperOrder> rows = new ArrayList<>();
        for (int i = 0; i < 30; i++) rows.add(sell(0.02, null));
        when(orderRepository.findTop100ByStrategyNameAndSideOrderByCreatedAtDesc(STRATEGY, "SELL")).thenReturn(rows);
        assertEquals(0.02, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
    }

    @Test
    void allWinnersCapsAtMaximumAllLosersFloorsAtMinimum() {
        history(25, 0.01, 0, 0);
        assertEquals(0.05, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
        history(0, 0, 25, 0.01);
        assertEquals(0.005, service.kellyFraction(STRATEGY).doubleValue(), 1e-12);
    }
}
