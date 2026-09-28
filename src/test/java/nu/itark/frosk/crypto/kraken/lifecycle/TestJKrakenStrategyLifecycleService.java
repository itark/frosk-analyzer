package nu.itark.frosk.crypto.kraken.lifecycle;

import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.InvalidTransitionException;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.PromotionRefusedException;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.StrategyNotFoundException;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import nu.itark.frosk.repo.KrakenStrategyConfigRepository;
import nu.itark.frosk.repo.KrakenStrategyModeChangeRepository;
import nu.itark.frosk.service.CryptoIntradayStrategyRunner;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Promotion rules: the three hard criteria, the allowed transitions, and that a
 * rollback is never gated.
 */
public class TestJKrakenStrategyLifecycleService {

    private static final String STRATEGY = "CryptoShortIntradayStrategy";
    private static final String PREREG = "CryptoLiquiditySweepIntradayStrategy";

    private final CryptoIntradayStrategyRunner runner = mock(CryptoIntradayStrategyRunner.class);
    private final KrakenStrategyModeService modeService = mock(KrakenStrategyModeService.class);
    private final KrakenStrategyConfigRepository configRepo = mock(KrakenStrategyConfigRepository.class);
    private final KrakenStrategyModeChangeRepository auditRepo = mock(KrakenStrategyModeChangeRepository.class);
    private final KrakenFuturesPaperOrderRepository paperRepo = mock(KrakenFuturesPaperOrderRepository.class);
    private final IntradaySignalRepository signalRepo = mock(IntradaySignalRepository.class);
    private final LiveTradingGate gate = mock(LiveTradingGate.class);

    private final KrakenStrategyLifecycleService service = new KrakenStrategyLifecycleService(
            runner, modeService, configRepo, auditRepo, paperRepo, signalRepo, gate);

    {
        ReflectionTestUtils.setField(service, "minClosedTrades", 30);
        ReflectionTestUtils.setField(service, "minDaysRunning", 30);
        ReflectionTestUtils.setField(service, "maxDrawdownPct", BigDecimal.valueOf(15));
        when(runner.getStrategyNames()).thenReturn(List.of(STRATEGY, PREREG));
        when(runner.isEnabledByConfig(anyString())).thenReturn(true);
        when(configRepo.findById(anyString())).thenReturn(Optional.empty());
    }

    /** {@code trades} closed trades each returning {@code pnlPct}% on 100 USD, strategy running {@code days} days. */
    private void givenHistory(String strategy, int trades, double pnlPct, int days) {
        List<KrakenFuturesPaperOrder> closed = new ArrayList<>();
        for (int i = 0; i < trades; i++) {
            KrakenFuturesPaperOrder o = new KrakenFuturesPaperOrder();
            o.setUsdAmount(BigDecimal.valueOf(100));
            // alternate sign lightly so there is variance but the curve trends up
            o.setRealizedPnlUsd(BigDecimal.valueOf(i % 3 == 2 ? -pnlPct / 2 : pnlPct));
            closed.add(o);
        }
        when(paperRepo.findByStrategyNameAndStatusOrderByClosedAtAsc(strategy, "CLOSED")).thenReturn(closed);
        IntradaySignal first = new IntradaySignal(strategy, "PF_XBTUSD",
                Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond(), "SHRT", BigDecimal.ONE);
        when(signalRepo.findTopByStrategyNameOrderBySignalTimestampAsc(strategy)).thenReturn(Optional.of(first));
    }

    private void givenMode(String strategy, StrategyMode mode) {
        when(modeService.getMode(strategy)).thenReturn(mode);
    }

    @Test
    void readiness_listsEveryUnmetCriterion() {
        givenHistory(STRATEGY, 12, 1.0, 18);
        givenMode(STRATEGY, StrategyMode.PAPER);

        StrategyReadinessDTO r = service.readiness(STRATEGY);

        assertFalse(r.isReadyForPromotion());
        assertEquals(12, r.getClosedTrades());
        assertEquals(18, r.getDaysRunning());
        assertTrue(r.getBlockers().contains("Minst 30 avslutade trades krävs (12/30)"), r.getBlockers().toString());
        assertTrue(r.getBlockers().contains("Minst 30 dagar krävs (18/30)"), r.getBlockers().toString());
    }

    @Test
    void promote_refusedWith422Payload_whenCriteriaUnmet_andModeUnchanged() {
        givenHistory(STRATEGY, 12, 1.0, 18);
        givenMode(STRATEGY, StrategyMode.PAPER);

        PromotionRefusedException e = assertThrows(PromotionRefusedException.class,
                () -> service.promote(STRATEGY, StrategyMode.LIVE));

        assertEquals(2, e.getReadiness().getBlockers().size());
        verify(modeService, never()).setMode(anyString(), any());
    }

    @Test
    void promote_succeeds_whenAllCriteriaMet() {
        givenHistory(STRATEGY, 40, 1.0, 45);
        givenMode(STRATEGY, StrategyMode.PAPER);

        service.promote(STRATEGY, StrategyMode.SHADOW);

        verify(modeService).setMode(STRATEGY, StrategyMode.SHADOW);
        verify(auditRepo).save(any());
    }

    @Test
    void promote_refused_whenDrawdownTooDeep() {
        // 40 straight −1% trades: compounded drawdown ≈ 33%.
        givenHistory(STRATEGY, 40, -1.0, 45);
        givenMode(STRATEGY, StrategyMode.PAPER);

        PromotionRefusedException e = assertThrows(PromotionRefusedException.class,
                () -> service.promote(STRATEGY, StrategyMode.SHADOW));

        assertTrue(e.getReadiness().getBlockers().get(0).startsWith("Max drawdown"), e.getReadiness().getBlockers().toString());
    }

    @Test
    void promote_refused_forPreRegisteredExperiment_evenWithGreatMetrics() {
        givenHistory(PREREG, 100, 2.0, 90);
        givenMode(PREREG, StrategyMode.PAPER);

        PromotionRefusedException e = assertThrows(PromotionRefusedException.class,
                () -> service.promote(PREREG, StrategyMode.LIVE));

        assertTrue(e.getReadiness().getBlockers().get(0).startsWith("Pre-registrerat"));
    }

    @Test
    void promote_shadowToLive_allowed_liveToShadow_isNotAPromotion() {
        givenHistory(STRATEGY, 40, 1.0, 45);

        givenMode(STRATEGY, StrategyMode.SHADOW);
        service.promote(STRATEGY, StrategyMode.LIVE);
        verify(modeService).setMode(STRATEGY, StrategyMode.LIVE);

        givenMode(STRATEGY, StrategyMode.LIVE);
        assertThrows(InvalidTransitionException.class, () -> service.promote(STRATEGY, StrategyMode.SHADOW));
    }

    @Test
    void promote_rejectsNonPromotionTargets() {
        givenMode(STRATEGY, StrategyMode.PAPER);
        assertThrows(InvalidTransitionException.class, () -> service.promote(STRATEGY, StrategyMode.PAPER));
        assertThrows(InvalidTransitionException.class, () -> service.promote(STRATEGY, null));
    }

    @Test
    void demote_toPaper_isNeverGated() {
        // Terrible metrics — rollback must still go through.
        givenHistory(STRATEGY, 3, -10.0, 2);
        givenMode(STRATEGY, StrategyMode.LIVE);

        service.demote(STRATEGY, StrategyMode.PAPER);

        verify(modeService).setMode(STRATEGY, StrategyMode.PAPER);
    }

    @Test
    void unknownStrategy_isNotFound() {
        assertThrows(StrategyNotFoundException.class, () -> service.readiness("NoSuchStrategy"));
    }
}
