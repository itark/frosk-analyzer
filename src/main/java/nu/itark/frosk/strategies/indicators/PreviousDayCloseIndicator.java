package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Returns the closing price of the last bar of the previous trading day.
 *
 * <p>Used by intraday strategies that need to detect overnight gaps
 * (e.g. GapReversalIntradayStrategy compares today's open to yesterday's close).
 *
 * <p>For the very first day in the series (no previous day exists), returns the
 * current bar's close as a safe fallback (gap = 0%).
 */
public class PreviousDayCloseIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    public PreviousDayCloseIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        BarSeries series = getBarSeries();
        ZonedDateTime currentTime = series.getBar(index).getEndTime().withZoneSameInstant(STOCKHOLM);
        int currentDay = currentTime.getDayOfYear();
        int currentYear = currentTime.getYear();

        // Walk backwards to find the last bar of the previous day
        for (int i = index - 1; i >= 0; i--) {
            ZonedDateTime t = series.getBar(i).getEndTime().withZoneSameInstant(STOCKHOLM);
            if (t.getDayOfYear() != currentDay || t.getYear() != currentYear) {
                // This is the last bar of a previous day
                return series.getBar(i).getClosePrice();
            }
        }

        // No previous day found — return current close as fallback
        return series.getBar(index).getClosePrice();
    }

    @Override
    public int getUnstableBars() {
        return 1;
    }
}
