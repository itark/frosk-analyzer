package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Trade-sign imbalance proxy for a single bar: {@code (close - open) / (high - low)}.
 *
 * <p>Ranges from -1 (bar closed at its low — sellers dominated) to +1 (bar
 * closed at its high — buyers dominated); 0 for a doji-like bar. Cheap
 * confirmation that the bar's own trading, not just the EMA cross, agrees with
 * the entry direction — no order-book data required, unlike a true order-flow
 * imbalance.
 *
 * <p>Zero range (high == low, e.g. a single-trade or illiquid bar) returns 0
 * (neutral) rather than dividing by zero.
 */
public class BarImbalanceIndicator extends CachedIndicator<Num> {

    public BarImbalanceIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        Bar bar = getBarSeries().getBar(index);
        Num range = bar.getHighPrice().minus(bar.getLowPrice());
        if (range.isZero()) {
            return numOf(0);
        }
        return bar.getClosePrice().minus(bar.getOpenPrice()).dividedBy(range);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
