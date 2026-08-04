package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.PreviousDayExtremeIndicator;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the entry rule against PREREG_liquidity_sweep_15m.md §4 on synthetic
 * 15m bars. Each test isolates one clause of the specification, so a regression
 * names the clause it broke.
 */
public class TestJLiquiditySweep {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final int BARS_PER_DAY = 96;

    /** Day 1 ranges 100..110; day 2 is built by the caller bar by bar. */
    private BarSeries build(double[][] day2) {
        BarSeries s = new BaseBarSeriesBuilder().withName("SWEEP").build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 15, 0, 0, UTC);
        for (int i = 0; i < BARS_PER_DAY; i++) {
            // previous day: low 100, high 110
            double lo = (i == 10) ? 100 : 104;
            double hi = (i == 20) ? 110 : 106;
            s.addBar(Duration.ofMinutes(15), t, 105, hi, lo, 105, 1000);
            t = t.plusMinutes(15);
        }
        for (double[] b : day2) {
            s.addBar(Duration.ofMinutes(15), t, b[0], b[1], b[2], b[3], 1000);
            t = t.plusMinutes(15);
        }
        return s;
    }

    private LiquiditySweepEntryRule rule(BarSeries s, double minRangePct) {
        return new LiquiditySweepEntryRule(s,
                new PreviousDayExtremeIndicator(s, UTC, true),
                new PreviousDayExtremeIndicator(s, UTC, false),
                UTC, 4, minRangePct);
    }

    @Test
    public void previousDayExtremesAreTheUtcDayHighAndLow() {
        BarSeries s = build(new double[][]{{105, 106, 104, 105}});
        int last = s.getEndIndex();
        assertEquals(110.0, new PreviousDayExtremeIndicator(s, UTC, true).getValue(last).doubleValue(), 1e-9);
        assertEquals(100.0, new PreviousDayExtremeIndicator(s, UTC, false).getValue(last).doubleValue(), 1e-9);
    }

    @Test
    public void firesOnSweepThenReclaim() {
        // bar0 dips to 99 (below PDL 100), bar1 closes back at 101
        BarSeries s = build(new double[][]{{102, 102, 99, 99.5}, {99.5, 101.5, 99.4, 101}});
        assertTrue(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "sweep below PDL followed by a close back above it must fire");
    }

    @Test
    public void doesNotFireWithoutASweep() {
        // never trades below PDL 100
        BarSeries s = build(new double[][]{{102, 103, 101, 102}, {102, 103, 101, 102.5}});
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "no penetration of PDL means no liquidity was swept");
    }

    @Test
    public void doesNotFireWhileStillBelowPdl() {
        // swept and stayed under — the reclaim has not happened
        BarSeries s = build(new double[][]{{102, 102, 99, 99.5}, {99.5, 99.8, 99.0, 99.4}});
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "a close below PDL is not a reclaim");
    }

    @Test
    public void doesNotFireWhenSweepIsOlderThanTheConfirmationWindow() {
        // sweep at bar0, reclaim only at bar6 — outside the 4-bar window
        BarSeries s = build(new double[][]{
                {102, 102, 99, 99.5}, {99.5, 99.9, 99.4, 99.6}, {99.6, 99.9, 99.5, 99.7},
                {99.7, 99.9, 99.6, 99.8}, {99.8, 99.9, 99.7, 99.85}, {99.85, 99.95, 99.8, 99.9},
                {99.9, 101.5, 99.9, 101}});
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "reclaim must occur within confirmBars of the sweep");
    }

    @Test
    public void doesNotFireWhenTargetAlreadyTakenToday() {
        // price tagged PDH 110 earlier in the day — no trade left to make
        BarSeries s = build(new double[][]{
                {105, 111, 104, 106}, {106, 106, 99, 99.5}, {99.5, 101.5, 99.4, 101}});
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "if PDH is already tagged the target is gone");
    }

    @Test
    public void doesNotFireWhenPreviousDayRangeIsTooNarrow() {
        // PDH/PDL = 110/100 = 10% range; demand 20% so the screen must block it
        BarSeries s = build(new double[][]{{102, 102, 99, 99.5}, {99.5, 101.5, 99.4, 101}});
        assertFalse(rule(s, 20.0).isSatisfied(s.getEndIndex(), null),
                "range screen must block targets that sit inside the noise");
    }

    @Test
    public void firesOnlyOncePerDay() {
        // two identical sweep+reclaim sequences in the same UTC day
        BarSeries s = build(new double[][]{
                {102, 102, 99, 99.5}, {99.5, 101.5, 99.4, 101},
                {101, 101.5, 99, 99.5}, {99.5, 101.5, 99.4, 101}});
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "a second setup in the same UTC day must be ignored");
    }

    @Test
    public void blocksWhenNoCompletePreviousDayExists() {
        BarSeries s = new BaseBarSeriesBuilder().withName("SHORT").build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 15, 0, 0, UTC);
        for (int i = 0; i < 5; i++) {
            s.addBar(Duration.ofMinutes(15), t, 105, 106, 99, 101, 1000);
            t = t.plusMinutes(15);
        }
        assertFalse(rule(s, 2.0).isSatisfied(s.getEndIndex(), null),
                "with no previous day the levels are undefined and must not trade");
    }
}
