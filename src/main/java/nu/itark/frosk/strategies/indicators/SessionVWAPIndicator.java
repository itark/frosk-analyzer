package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Session-anchored Volume-Weighted Average Price.
 *
 * <p>Computes Σ(typicalPrice × volume) / Σ(volume) over all bars belonging to
 * the current trading day (Stockholm time), resetting at every session start.
 * This is the VWAP that intraday traders actually reference — unlike a rolling
 * SMA/MVWAP, the anchor does not drift across the day.
 *
 * <p>Falls back to the close price when the cumulative volume is zero
 * (e.g. index series without volume data).
 */
public class SessionVWAPIndicator extends CachedIndicator<Num> {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    public SessionVWAPIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        BarSeries series = getBarSeries();
        ZonedDateTime currentTime = series.getBar(index).getEndTime().withZoneSameInstant(STOCKHOLM);
        int currentDay = currentTime.getDayOfYear();
        int currentYear = currentTime.getYear();

        Num cumulativePV = numOf(0);
        Num cumulativeVolume = numOf(0);
        for (int i = index; i >= 0; i--) {
            ZonedDateTime t = series.getBar(i).getEndTime().withZoneSameInstant(STOCKHOLM);
            if (t.getDayOfYear() != currentDay || t.getYear() != currentYear) {
                break;
            }
            Bar bar = series.getBar(i);
            Num typicalPrice = bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice())
                    .dividedBy(numOf(3));
            cumulativePV = cumulativePV.plus(typicalPrice.multipliedBy(bar.getVolume()));
            cumulativeVolume = cumulativeVolume.plus(bar.getVolume());
        }

        if (cumulativeVolume.isZero()) {
            return series.getBar(index).getClosePrice();
        }
        return cumulativePV.dividedBy(cumulativeVolume);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
