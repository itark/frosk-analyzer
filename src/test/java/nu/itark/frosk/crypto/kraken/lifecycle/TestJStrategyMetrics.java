package nu.itark.frosk.crypto.kraken.lifecycle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestJStrategyMetrics {

    private static StrategyMetrics.Trade t(double pnl) {
        // 100 USD notional, so pnl in USD == return in percent.
        return new StrategyMetrics.Trade(BigDecimal.valueOf(pnl), BigDecimal.valueOf(100));
    }

    @Test
    void noTrades_allMetricsUndefined() {
        StrategyMetrics m = StrategyMetrics.of(List.of());

        assertEquals(0, m.closedTrades());
        assertNull(m.maxDrawdownPct());
        assertNull(m.sharpeRatio());
        assertNull(m.winRate());
        assertNull(m.avgRR());
    }

    @Test
    void drawdown_isPeakToTroughOfCompoundedReturns() {
        // +10% → 1.10 (peak), −20% → 0.88, −10% → 0.792, +5% → 0.8316
        // max DD = (1.10 − 0.792) / 1.10 = 28.00 %
        StrategyMetrics m = StrategyMetrics.of(List.of(t(10), t(-20), t(-10), t(5)));

        assertEquals(new BigDecimal("28.00"), m.maxDrawdownPct());
    }

    @Test
    void winRateAndRR() {
        // wins 4, 2 (avg 3) ; losses −1, −2 (avg 1.5) → RR 2.00, win rate 0.5
        StrategyMetrics m = StrategyMetrics.of(List.of(t(4), t(-1), t(2), t(-2)));

        assertEquals(new BigDecimal("0.5000"), m.winRate());
        assertEquals(new BigDecimal("2.00"), m.avgRR());
        assertEquals(new BigDecimal("3.00"), m.totalPnlUsd());
    }

    @Test
    void sharpe_isMeanOverSampleStdev() {
        // returns 0.01, 0.03 → mean 0.02, sample stdev 0.014142 → 1.41
        StrategyMetrics m = StrategyMetrics.of(List.of(t(1), t(3)));

        assertEquals(new BigDecimal("1.41"), m.sharpeRatio());
    }

    @Test
    void onlyWins_rrUndefined_noDrawdown() {
        StrategyMetrics m = StrategyMetrics.of(List.of(t(1), t(2)));

        assertNull(m.avgRR());
        assertEquals(new BigDecimal("0.00"), m.maxDrawdownPct());
    }
}
