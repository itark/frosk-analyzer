package nu.itark.frosk.service;

import nu.itark.frosk.analysis.BtcTrendSignal;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure unit test (no Spring context) for the BTC Trend Follower's three-state
 * rule. Proves the signal is genuinely dynamic across all three branches —
 * LONG / SHORT / NEUTRAL — both at the decision-function level
 * ({@link CryptoBtcTrendSignalService#classify}) and end-to-end through the
 * real ta4j EMA/ADX indicators ({@link CryptoBtcTrendSignalService#signalAt}).
 */
class TestJCryptoBtcTrendSignal {

    private static final double THR = 20.0;

    // ── classify(): the branch logic in isolation ────────────────────────────

    @Test
    void classify_long_whenFastAboveSlow_andTrendStrong() {
        assertEquals(BtcTrendSignal.LONG, CryptoBtcTrendSignalService.classify(67568, 66334, 36.96, THR));
    }

    @Test
    void classify_short_whenFastBelowSlow_andTrendStrong() {
        assertEquals(BtcTrendSignal.SHORT, CryptoBtcTrendSignalService.classify(64000, 66000, 27.5, THR));
    }

    @Test
    void classify_neutral_whenAdxBelowThreshold_regardlessOfEmaOrder() {
        assertEquals(BtcTrendSignal.NEUTRAL, CryptoBtcTrendSignalService.classify(67000, 66000, 19.99, THR));
        assertEquals(BtcTrendSignal.NEUTRAL, CryptoBtcTrendSignalService.classify(64000, 66000, 3.0, THR));
    }

    @Test
    void classify_adxExactlyAtThreshold_countsAsTrending() {
        assertEquals(BtcTrendSignal.LONG, CryptoBtcTrendSignalService.classify(67000, 66000, THR, THR));
        assertEquals(BtcTrendSignal.SHORT, CryptoBtcTrendSignalService.classify(65000, 66000, THR, THR));
    }

    @Test
    void classify_equalEmas_resolveToShort() {
        assertEquals(BtcTrendSignal.SHORT, CryptoBtcTrendSignalService.classify(66000, 66000, 40.0, THR));
    }

    // ── signalAt(): end-to-end through real EMA/ADX indicators ───────────────

    @Test
    void signalAt_long_onSustainedUptrend() {
        BarSeries s = ramp(160, 20_000, +250);   // strong, steady climb
        assertEquals(BtcTrendSignal.LONG, evalLast(s));
    }

    @Test
    void signalAt_short_onSustainedDowntrend() {
        BarSeries s = ramp(160, 80_000, -250);   // strong, steady fall
        assertEquals(BtcTrendSignal.SHORT, evalLast(s));
    }

    @Test
    void signalAt_neutral_onFlatChop() {
        BarSeries s = chop(200, 50_000, 120);     // oscillates, no trend → low ADX
        assertEquals(BtcTrendSignal.NEUTRAL, evalLast(s));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static String evalLast(BarSeries series) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator fast = new EMAIndicator(close, 10);
        EMAIndicator slow = new EMAIndicator(close, 20);
        ADXIndicator adx = new ADXIndicator(series, 14);
        return CryptoBtcTrendSignalService.signalAt(series.getEndIndex(), fast, slow, adx, THR);
    }

    private static BarSeries ramp(int bars, double start, double step) {
        BarSeries s = new BaseBarSeriesBuilder().withName("test").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        double c = start;
        for (int i = 0; i < bars; i++) {
            double open = c;
            c += step;
            double hi = Math.max(open, c) + Math.abs(step) * 0.2;
            double lo = Math.min(open, c) - Math.abs(step) * 0.2;
            s.addBar(t.plusDays(i), open, hi, lo, c, 1.0);
        }
        return s;
    }

    private static BarSeries chop(int bars, double mid, double amp) {
        BarSeries s = new BaseBarSeriesBuilder().withName("test").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        for (int i = 0; i < bars; i++) {
            double c = mid + (i % 2 == 0 ? amp : -amp);
            double open = mid + (i % 2 == 0 ? -amp : amp);
            s.addBar(t.plusDays(i), open, Math.max(open, c) + 5, Math.min(open, c) - 5, c, 1.0);
        }
        return s;
    }
}
