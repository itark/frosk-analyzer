package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class MultipliedIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> indicator;
    private final Num multiplier;

    /**
     * Constructor
     * @param indicator the base indicator
     * @param multiplier the multiplier value
     */
    public MultipliedIndicator(Indicator<Num> indicator, Num multiplier) {
        super(indicator);
        this.indicator = indicator;
        this.multiplier = multiplier;
    }

    @Override
    protected Num calculate(int index) {
        return indicator.getValue(index).multipliedBy(multiplier);
    }

    @Override
    public int getUnstableBars() {
        return indicator.getUnstableBars();
    }

}
