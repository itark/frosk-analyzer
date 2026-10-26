package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.time.ZoneOffset;

/**
 * Allows entry only while bar {@code index}'s end time falls in the UTC hour
 * window [{@code startHour}, {@code endHour}) — the allow-only counterpart of
 * {@link TimeGatingRule}, which blocks a window instead. Handles windows that
 * wrap midnight ({@code startHour > endHour}, e.g. 22 → 2).
 *
 * <p>Like {@link TimeGatingRule}, it reads the bar's own end time, never the
 * wall clock, so the {@code FeaturedStrategy} backtest replay gates every
 * historical bar on its own time of day.
 */
public class TimeWindowRule extends AbstractRule {

    private final BarSeries barSeries;
    private final int startHour;
    private final int endHour;

    public TimeWindowRule(BarSeries barSeries, int startHour, int endHour) {
        if (startHour < 0 || startHour > 23 || endHour < 0 || endHour > 24 || startHour == endHour) {
            throw new IllegalArgumentException("Invalid UTC hour window [" + startHour + ", " + endHour + ")");
        }
        this.barSeries = barSeries;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int hour = barSeries.getBar(index).getEndTime().withZoneSameInstant(ZoneOffset.UTC).getHour();
        boolean satisfied = startHour < endHour
                ? hour >= startHour && hour < endHour
                : hour >= startHour || hour < endHour;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
