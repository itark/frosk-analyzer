package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Running low of the current trading day up to the current bar.
 *
 * <p>Used by gap-reversal strategies to detect whether the first bar's low
 * holds as support (i.e. "no new low" condition after the opening bar).
 */
public class DayLowIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    public DayLowIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        BarSeries series = getBarSeries();
        ZonedDateTime currentTime = series.getBar(index).getEndTime().withZoneSameInstant(STOCKHOLM);
        int currentDay = currentTime.getDayOfYear();
        int currentYear = currentTime.getYear();

        Num dayLow = series.getBar(index).getLowPrice();

        for (int i = index - 1; i >= 0; i--) {
            ZonedDateTime t = series.getBar(i).getEndTime().withZoneSameInstant(STOCKHOLM);
            if (t.getDayOfYear() == currentDay && t.getYear() == currentYear) {
                Num low = series.getBar(i).getLowPrice();
                if (low.isLessThan(dayLow)) {
                    dayLow = low;
                }
            } else {
                break;
            }
        }
        return dayLow;
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
