package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class LogReturnIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> price;

    public LogReturnIndicator(Indicator<Num> price) {
        super(price);
        this.price = price;
    }

    @Override
    protected Num calculate(int index) {
        if (index == 0) {
            return numOf(0);
        }
        Num current = null;
        try {
            current = price.getValue(index);
        } catch (Exception e) {
            //log.warn("index:{}, error:{}, continuing...",index, e.getMessage());
            return numOf(0);
        }
        Num previous = price.getValue(index - 1);

        if (previous.isZero()) {
            return numOf(0);
        }

        return current.dividedBy(previous).log();
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}

