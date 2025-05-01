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
public class VixRiskOffIndicator extends CachedIndicator<Boolean> {
    Num threshold = DoubleNum.valueOf(25);

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public VixRiskOffIndicator(BarSeries series) {
         super(series);
     }

    @Override
    protected Boolean calculate(int index) {
        if (index < 1) {
            // GAP is a 2-candle pattern
            return false;
        }
        Bar currBar = getBarSeries().getBar(index);
        if (currBar.isBullish()) {
            return currBar.getClosePrice().isGreaterThanOrEqual(threshold);
        }
        return false;
    }


    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
