package nu.itark.frosk.strategies;

import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure unit test (no Spring context) for the session-anchored VWAP:
 * cumulative within a day, reset at the day boundary, close-price fallback
 * when volume is zero.
 */
public class TestJSessionVWAPIndicator {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Test
    public void resetsAtDayBoundaryAndAccumulatesWithinDay() {
        BarSeries series = new BaseBarSeriesBuilder().withName("test")
                .withNumTypeOf(DoubleNum.class).build();

        ZonedDateTime day1 = ZonedDateTime.of(2026, 6, 10, 9, 15, 0, 0, STOCKHOLM);
        // Day 1, bar 0: H=12 L=8 C=10 → typical 10, vol 100
        addBar(series, day1, 10, 12, 8, 10, 100);
        // Day 1, bar 1: H=22 L=18 C=20 → typical 20, vol 300
        addBar(series, day1.plusMinutes(15), 20, 22, 18, 20, 300);

        ZonedDateTime day2 = day1.plusDays(1);
        // Day 2, bar 2: H=32 L=28 C=30 → typical 30, vol 100
        addBar(series, day2, 30, 32, 28, 30, 100);

        SessionVWAPIndicator vwap = new SessionVWAPIndicator(series);

        // Bar 0: VWAP = 10
        assertEquals(10.0, vwap.getValue(0).doubleValue(), 1e-9);
        // Bar 1: (10*100 + 20*300) / 400 = 17.5
        assertEquals(17.5, vwap.getValue(1).doubleValue(), 1e-9);
        // Bar 2 is a new day: VWAP resets to 30, not influenced by day 1
        assertEquals(30.0, vwap.getValue(2).doubleValue(), 1e-9);
    }

    @Test
    public void fallsBackToCloseWhenNoVolume() {
        BarSeries series = new BaseBarSeriesBuilder().withName("test")
                .withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 6, 10, 9, 15, 0, 0, STOCKHOLM);
        addBar(series, t, 100, 110, 90, 105, 0);

        SessionVWAPIndicator vwap = new SessionVWAPIndicator(series);
        assertEquals(105.0, vwap.getValue(0).doubleValue(), 1e-9);
    }

    private void addBar(BarSeries series, ZonedDateTime endTime,
                        double open, double high, double low, double close, double volume) {
        series.addBar(Duration.ofMinutes(15), endTime, open, high, low, close, volume);
    }
}
