package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * https://www.investopedia.com/terms/v/vix.asp
 *
 * Part of Risk-off score
 *
 */
public class VixRiskIndicator extends CachedIndicator<Boolean> {
    Num threshold = DoubleNum.valueOf(25);

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public VixRiskIndicator(BarSeries series) {
         super(series);
     }

    @Override
    protected Boolean calculate(int index) {
        Bar currBar = getBarSeries().getBar(index);
        return currBar.isBullish() && currBar.getClosePrice().isGreaterThanOrEqual(threshold);
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
