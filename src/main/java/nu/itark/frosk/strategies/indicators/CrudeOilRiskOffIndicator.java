package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

/**

 * Part of Risk-off score
 *
 */
@Slf4j
public class CrudeOilRiskOffIndicator extends CachedIndicator<Boolean> {
    Num threshold = DoubleNum.valueOf(-5);

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public CrudeOilRiskOffIndicator(BarSeries series) {
         super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < 5) {
            return false;
        }
        Bar prevBar;
        Bar currBar;
        try {
            prevBar = getBarSeries().getBar(index - 5);
            currBar = getBarSeries().getBar(index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final Num percentage = prevBar.getClosePrice().minus(currBar.getClosePrice()).dividedBy(prevBar.getClosePrice()).multipliedBy(DoubleNum.valueOf(100));
        if (percentage.isLessThan(threshold)) {
            return true;
        }
        return false;
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
