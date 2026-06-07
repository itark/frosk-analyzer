package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Counts how many bars have elapsed since the start of the current trading day.
 *
 * <p>Returns 0 for the first bar of the day, 1 for the second, etc.
 * Used to implement time-of-day filters (e.g. "only trade after bar 2"
 * or "no new entries after bar 28").
 */
public class IntradayBarCountIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    public IntradayBarCountIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        BarSeries series = getBarSeries();
        ZonedDateTime currentTime = series.getBar(index).getEndTime().withZoneSameInstant(STOCKHOLM);
        int currentDay = currentTime.getDayOfYear();
        int currentYear = currentTime.getYear();

        int count = 0;
        for (int i = index - 1; i >= 0; i--) {
            ZonedDateTime t = series.getBar(i).getEndTime().withZoneSameInstant(STOCKHOLM);
            if (t.getDayOfYear() == currentDay && t.getYear() == currentYear) {
                count++;
            } else {
                break;
            }
        }
        return numOf(count);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
