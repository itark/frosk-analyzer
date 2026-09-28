package nu.itark.frosk.crypto.kraken.lifecycle;

import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import nu.itark.frosk.repo.KrakenStrategyConfigRepository;
import nu.itark.frosk.repo.KrakenStrategyModeChangeRepository;
import nu.itark.frosk.service.CryptoIntradayStrategyRunner;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The regime split on the readiness payload — the evidence a decision about a
 * regime filter should rest on, rather than intuition.
 */
public class TestJRegimeBreakdown {

    private static final String STRATEGY = "CryptoVWAPReversionIntradayStrategy";

    private static KrakenFuturesPaperOrder closed(String regime, String pnl) {
        KrakenFuturesPaperOrder o = new KrakenFuturesPaperOrder();
        o.setStrategyName(STRATEGY);
        o.setStatus("CLOSED");
        o.setUsdAmount(new BigDecimal("250"));
        o.setRealizedPnlUsd(new BigDecimal(pnl));
        o.setMarketRegime(regime);
        return o;
    }

    private StrategyReadinessDTO readinessFor(List<KrakenFuturesPaperOrder> orders) {
        KrakenFuturesPaperOrderRepository paperRepo = mock(KrakenFuturesPaperOrderRepository.class);
        when(paperRepo.findByStrategyNameAndStatusOrderByClosedAtAsc(STRATEGY, "CLOSED")).thenReturn(orders);
        when(paperRepo.findTopByStrategyNameOrderByCreatedAtAsc(STRATEGY)).thenReturn(Optional.empty());

        CryptoIntradayStrategyRunner runner = mock(CryptoIntradayStrategyRunner.class);
        when(runner.getStrategyNames()).thenReturn(List.of(STRATEGY));
        when(runner.isEnabledByConfig(anyString())).thenReturn(true);

        KrakenStrategyConfigRepository configRepo = mock(KrakenStrategyConfigRepository.class);
        when(configRepo.findById(anyString())).thenReturn(Optional.empty());
        KrakenStrategyModeService modeService = mock(KrakenStrategyModeService.class);
        when(modeService.getMode(STRATEGY)).thenReturn(StrategyMode.PAPER);
        IntradaySignalRepository signals = mock(IntradaySignalRepository.class);
        when(signals.findTopByStrategyNameOrderBySignalTimestampAsc(STRATEGY)).thenReturn(Optional.empty());

        KrakenStrategyLifecycleService service = new KrakenStrategyLifecycleService(
                runner, modeService, configRepo, mock(KrakenStrategyModeChangeRepository.class),
                paperRepo, signals, mock(LiveTradingGate.class));
        ReflectionTestUtils.setField(service, "minClosedTrades", 30);
        ReflectionTestUtils.setField(service, "minDaysRunning", 30);
        ReflectionTestUtils.setField(service, "maxDrawdownPct", BigDecimal.valueOf(15));
        return service.buildReadiness(STRATEGY);
    }

    @Test
    void tradesAreSplitPerRegime_sortedAndAggregated() {
        StrategyReadinessDTO r = readinessFor(List.of(
                closed("TRENDING_UP", "10"), closed("TRENDING_UP", "-4"), closed("TRENDING_UP", "6"),
                closed("RANGING", "3"), closed("RANGING", "-1")));

        assertEquals(List.of("RANGING", "TRENDING_UP"), r.getByRegime().stream().map(x -> x.getRegime()).toList());

        StrategyReadinessDTO.RegimeStats ranging = r.getByRegime().get(0);
        assertEquals(2, ranging.getClosedTrades());
        assertEquals(new BigDecimal("2.00"), ranging.getTotalPnlUsd());
        assertEquals(new BigDecimal("0.5000"), ranging.getWinRate());

        StrategyReadinessDTO.RegimeStats up = r.getByRegime().get(1);
        assertEquals(3, up.getClosedTrades());
        assertEquals(new BigDecimal("12.00"), up.getTotalPnlUsd());
        assertEquals(new BigDecimal("0.6667"), up.getWinRate());
    }

    @Test
    void tradesFromBeforeTheLogging_groupUnderUnknown() {
        StrategyReadinessDTO r = readinessFor(List.of(closed(null, "5"), closed("RANGING", "2")));

        assertEquals(List.of("RANGING", "UNKNOWN"), r.getByRegime().stream().map(x -> x.getRegime()).toList());
        assertEquals(1, r.getByRegime().get(1).getClosedTrades());
    }

    @Test
    void noTrades_noBreakdown() {
        assertEquals(List.of(), readinessFor(List.of()).getByRegime());
    }
}
