package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-off when DXY > 105 AND bar is bullish (rising).
 * A strong, rising dollar tightens global financial conditions → equity risk-off.
 */
public class DXYRiskOffIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(105);

    public DXYRiskOffIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        Bar currBar = getBarSeries().getBar(index);
        return currBar.isBullish() && currBar.getClosePrice().isGreaterThanOrEqual(threshold);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
