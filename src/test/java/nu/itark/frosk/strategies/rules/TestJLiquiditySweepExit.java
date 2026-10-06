package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.PreviousDayExtremeIndicator;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the exit rule's stop level (PREREG_liquidity_sweep_15m.md §5) under both
 * trade execution models. Regression for the live bug where, under
 * {@code TradeOnNextOpenModel}, the stop was derived from the fill bar instead of the
 * reclaim bar, came out null, and positions rode to the 96-bar time stop
 * (TRAC-USD 2026-10-05: stop 0.4399 breached within 3 bars, held 27.5h, −7.4%).
 */
public class TestJLiquiditySweepExit {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final int BARS_PER_DAY = 96;
    /** Index of the reclaim (signal) bar in {@link #build}. */
    private static final int SIGNAL = BARS_PER_DAY + 1;

    /**
     * Day 1 ranges 100..110 (PDL 100, PDH 110). Day 2: bar0 sweeps to 99, bar1
     * reclaims at 101 (signal, sweep low = 99), then the caller's bars follow.
     */
    private BarSeries build(double[]... after) {
        BarSeries s = new BaseBarSeriesBuilder().withName("SWEEP").build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 15, 0, 0, UTC);
        for (int i = 0; i < BARS_PER_DAY; i++) {
            double lo = (i == 10) ? 100 : 104;
            double hi = (i == 20) ? 110 : 106;
            s.addBar(Duration.ofMinutes(15), t, 105, hi, lo, 105, 1000);
            t = t.plusMinutes(15);
        }
        double[][] day2 = new double[2 + after.length][];
        day2[0] = new double[]{102, 102, 99, 99.5};
        day2[1] = new double[]{99.5, 101.5, 99.4, 101};
        System.arraycopy(after, 0, day2, 2, after.length);
        for (double[] b : day2) {
            s.addBar(Duration.ofMinutes(15), t, b[0], b[1], b[2], b[3], 1000);
            t = t.plusMinutes(15);
        }
        return s;
    }

    private LiquiditySweepExitRule exitRule(BarSeries s, boolean entryOnNextOpen) {
        return new LiquiditySweepExitRule(s,
                new PreviousDayExtremeIndicator(s, UTC, true),
                new PreviousDayExtremeIndicator(s, UTC, false),
                4, 96, entryOnNextOpen);
    }

    private TradingRecord openedAt(BarSeries s, int entryIndex) {
        TradingRecord record = new BaseTradingRecord();
        record.enter(entryIndex, s.getBar(entryIndex).getOpenPrice(), s.numOf(1));
        return record;
    }

    private static final double[] FILL_BAR = {101, 102, 100.5, 101.5};
    private static final double[] HOLDS = {101.5, 101.8, 99.5, 100.5};
    private static final double[] BREACH = {100.5, 100.8, 98.9, 99.2};

    @Test
    public void nextOpenStopIsTheSweepLowOfTheReclaimBar() {
        BarSeries s = build(FILL_BAR);
        Num stop = exitRule(s, true).stopLevel(SIGNAL + 1);
        assertNotNull(stop, "next-open fill must still find the sweep that produced the signal");
        assertEquals(99.0, stop.doubleValue(), 1e-9);
    }

    @Test
    public void readingTheFillBarAsSignalBarLosesTheStop() {
        // Documents the pre-fix behaviour: the fill bar terminates no excursion.
        BarSeries s = build(FILL_BAR);
        assertNull(exitRule(s, false).stopLevel(SIGNAL + 1));
    }

    @Test
    public void currentCloseStopIsUnchanged() {
        BarSeries s = build();
        assertEquals(99.0, exitRule(s, false).stopLevel(SIGNAL).doubleValue(), 1e-9);
    }

    @Test
    public void nextOpenExitsOnlyWhenTheSweepLowIsBreached() {
        BarSeries s = build(FILL_BAR, HOLDS, BREACH);
        LiquiditySweepExitRule rule = exitRule(s, true);
        TradingRecord record = openedAt(s, SIGNAL + 1);
        assertFalse(rule.isSatisfied(SIGNAL + 2, record), "low 99.5 stays above the 99 stop");
        assertTrue(rule.isSatisfied(SIGNAL + 3, record), "low 98.9 breaches the 99 stop");
    }

    @Test
    public void defaultBarSeriesManagerClosesOnTheStop() {
        // Same wiring as CryptoIntradayStrategyRunner: default manager = next open.
        BarSeries s = build(FILL_BAR, HOLDS, BREACH, new double[]{99.2, 99.6, 99.0, 99.4});
        PreviousDayExtremeIndicator pdh = new PreviousDayExtremeIndicator(s, UTC, true);
        PreviousDayExtremeIndicator pdl = new PreviousDayExtremeIndicator(s, UTC, false);
        BaseStrategy strategy = new BaseStrategy(
                new LiquiditySweepEntryRule(s, pdh, pdl, UTC, 4, 2.0),
                new LiquiditySweepExitRule(s, pdh, pdl, 4, 96, true));

        TradingRecord record = new BarSeriesManager(s).run(strategy);

        assertEquals(1, record.getPositions().size(), "the stop must close the position");
        Position p = record.getPositions().get(0);
        assertEquals(SIGNAL + 1, p.getEntry().getIndex());
        assertEquals(SIGNAL + 4, p.getExit().getIndex(), "exit fills on the open after the breach bar");
    }
}
