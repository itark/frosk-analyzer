package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * High (PDH) or low (PDL) of the previous complete calendar day in {@code zone}.
 *
 * <p>Anchored to the UTC day for crypto: there is no exchange session, and UTC
 * midnight is the reference every venue and charting package uses, so it is where
 * resting stop orders actually cluster.
 *
 * <p>Returns zero until a complete previous day exists in the series — callers
 * must treat zero as "no level", never as a tradable price. A rule that compares
 * against a zero level would fire on every bar.
 */
public class PreviousDayExtremeIndicator extends CachedIndicator<Num> {

    private final ZoneId zone;
    private final boolean high;

    public PreviousDayExtremeIndicator(BarSeries series, ZoneId zone, boolean high) {
        super(series);
        this.zone = zone;
        this.high = high;
    }

    @Override
    protected Num calculate(int index) {
        LocalDate today = getBarSeries().getBar(index).getEndTime()
                .withZoneSameInstant(zone).toLocalDate();
        LocalDate prev = null;
        Num extreme = null;
        for (int i = index - 1; i >= 0; i--) {
            LocalDate d = getBarSeries().getBar(i).getEndTime()
                    .withZoneSameInstant(zone).toLocalDate();
            if (d.equals(today)) {
                continue;
            }
            if (prev == null) {
                prev = d;                       // first day found before today
            } else if (!d.equals(prev)) {
                break;                          // walked past it — done
            }
            Num v = high ? getBarSeries().getBar(i).getHighPrice()
                         : getBarSeries().getBar(i).getLowPrice();
            if (extreme == null) {
                extreme = v;
            } else if (high ? v.isGreaterThan(extreme) : v.isLessThan(extreme)) {
                extreme = v;
            }
        }
        return extreme == null ? numOf(0) : extreme;
    }

    @Override
    public int getUnstableBars() {
        return 96; // one full day of 15m bars
    }
}
