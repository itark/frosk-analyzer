package nu.itark.frosk.strategies.rules;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link TimeWindowRule} judges each bar on its own end time in UTC — one 15m bar per
 * quarter hour over a full UTC day, so index {@code i} ends at {@code (i+1) * 15} minutes.
 */
public class TestJTimeWindowRule {

    private static BarSeries day(ZoneId zone) {
        BarSeries series = new BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime start = ZonedDateTime.of(2026, 10, 16, 0, 0, 0, 0, ZoneId.of("UTC")).withZoneSameInstant(zone);
        for (int i = 0; i < 96; i++) {
            series.addBar(start.plus(Duration.ofMinutes(15L * (i + 1))), 1, 1, 1, 1, 1);
        }
        return series;
    }

    /** Index of the bar whose end time is hh:mm UTC. */
    private static int endingAt(int hh, int mm) {
        return (hh * 60 + mm) / 15 - 1;
    }

    @Test
    void allowsOnlyBarsEndingInsideWindow() {
        TimeWindowRule rule = new TimeWindowRule(day(ZoneId.of("UTC")), 21, 23);
        assertEquals(false, rule.isSatisfied(endingAt(20, 45), null));
        assertEquals(true, rule.isSatisfied(endingAt(21, 0), null));
        assertEquals(true, rule.isSatisfied(endingAt(22, 45), null));
        assertEquals(false, rule.isSatisfied(endingAt(23, 0), null));
        assertEquals(false, rule.isSatisfied(endingAt(12, 0), null));
    }

    @Test
    void usesUtcEvenWhenSeriesIsInAnotherZone() {
        TimeWindowRule rule = new TimeWindowRule(day(ZoneId.of("Europe/Stockholm")), 21, 23);
        assertEquals(true, rule.isSatisfied(endingAt(21, 30), null));   // 23:30 Stockholm
        assertEquals(false, rule.isSatisfied(endingAt(19, 30), null));  // 21:30 Stockholm
    }

    @Test
    void wrapsMidnight() {
        TimeWindowRule rule = new TimeWindowRule(day(ZoneId.of("UTC")), 22, 2);
        assertEquals(true, rule.isSatisfied(endingAt(22, 15), null));
        assertEquals(true, rule.isSatisfied(endingAt(1, 45), null));
        assertEquals(false, rule.isSatisfied(endingAt(2, 0), null));
        assertEquals(false, rule.isSatisfied(endingAt(12, 0), null));
    }

    @Test
    void rejectsEmptyOrInvalidWindow() {
        BarSeries series = day(ZoneId.of("UTC"));
        assertThrows(IllegalArgumentException.class, () -> new TimeWindowRule(series, 21, 21));
        assertThrows(IllegalArgumentException.class, () -> new TimeWindowRule(series, 24, 2));
    }
}
