package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-on when DXY is NOT (above 105 AND rising).
 * Complement of {@link DXYRiskOffIndicator}.
 */
public class DXYRiskOnIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(105);

    public DXYRiskOnIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        Bar currBar = getBarSeries().getBar(index);
        return !(currBar.isBullish() && currBar.getClosePrice().isGreaterThanOrEqual(threshold));
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
