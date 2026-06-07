package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Calculates the overnight gap as a percentage.
 *
 * <p>Gap% = (today's first bar open - previous day close) / previous day close * 100
 *
 * <p>A negative value indicates a gap-down; positive indicates a gap-up.
 * This value is constant for all bars within the same trading day (it's always
 * the gap from the day's open).
 */
public class GapPercentIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private final PreviousDayCloseIndicator prevDayClose;

    public GapPercentIndicator(BarSeries series) {
        super(series);
        this.prevDayClose = new PreviousDayCloseIndicator(series);
    }

    @Override
    protected Num calculate(int index) {
        BarSeries series = getBarSeries();
        ZonedDateTime currentTime = series.getBar(index).getEndTime().withZoneSameInstant(STOCKHOLM);
        int currentDay = currentTime.getDayOfYear();
        int currentYear = currentTime.getYear();

        // Find the first bar of the current trading day
        int dayStartIndex = index;
        for (int i = index - 1; i >= 0; i--) {
            ZonedDateTime t = series.getBar(i).getEndTime().withZoneSameInstant(STOCKHOLM);
            if (t.getDayOfYear() == currentDay && t.getYear() == currentYear) {
                dayStartIndex = i;
            } else {
                break;
            }
        }

        Num todayOpen = series.getBar(dayStartIndex).getOpenPrice();
        Num prevClose = prevDayClose.getValue(dayStartIndex);

        if (prevClose.isZero()) {
            return numOf(0);
        }

        return todayOpen.minus(prevClose).dividedBy(prevClose).multipliedBy(numOf(100));
    }

    @Override
    public int getUnstableBars() {
        return 1;
    }
}
