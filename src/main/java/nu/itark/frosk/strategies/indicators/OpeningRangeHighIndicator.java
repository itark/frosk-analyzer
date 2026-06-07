package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Tracks the highest high during the Opening Range (OR) period.
 *
 * <p>The Opening Range is defined as the first {@code orBars} bars of each trading day
 * (default: 2 bars = 30 minutes on 15-minute charts, covering 09:00–09:30 Stockholm time).
 *
 * <p>Once the OR is established, this indicator returns the OR high for all subsequent
 * bars on the same day. Before the OR is complete, it returns the running high.
 * On new days, the OR resets.
 */
public class OpeningRangeHighIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private final int orBars;

    /**
     * @param series the 15-minute bar series
     * @param orBars number of bars constituting the opening range (2 = 30 min)
     */
    public OpeningRangeHighIndicator(BarSeries series, int orBars) {
        super(series);
        this.orBars = orBars;
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

        // Calculate OR high from the first orBars of the day
        int orEnd = Math.min(dayStartIndex + orBars - 1, index);
        Num orHigh = series.getBar(dayStartIndex).getHighPrice();
        for (int i = dayStartIndex + 1; i <= orEnd; i++) {
            Num high = series.getBar(i).getHighPrice();
            if (high.isGreaterThan(orHigh)) {
                orHigh = high;
            }
        }
        return orHigh;
    }

    @Override
    public int getUnstableBars() {
        return orBars;
    }
}
