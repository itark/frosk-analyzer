package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Calculates percentage return over N bars.
 */
public class ReturnOverPeriodIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> price;
    private final int barCount;

    public ReturnOverPeriodIndicator(Indicator<Num> price, int barCount) {
        super(price);
        this.price = price;
        this.barCount = barCount;
    }

    @Override
    protected Num calculate(int index) {
        if (index < barCount) {
            return numOf(0);
        }
        Num current = price.getValue(index);
        Num past = price.getValue(index - barCount);
        return current.dividedBy(past).minus(numOf(1)); // (P_now / P_past) - 1
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
