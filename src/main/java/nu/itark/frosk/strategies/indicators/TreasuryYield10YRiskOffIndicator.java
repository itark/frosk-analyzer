package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-off when US 10-Year Treasury Yield (^TNX) is above 4.5% AND rising
 * (close > close 5 bars ago, i.e. 5-day ROC > 0).
 *
 * High and rising long-term rates lift Swedish long rates via global bond linkage
 * and compress equity valuations.
 */
public class TreasuryYield10YRiskOffIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(4.5);

    public TreasuryYield10YRiskOffIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < 5) return false;
        Num curr = getBarSeries().getBar(index).getClosePrice();
        Num prev = getBarSeries().getBar(index - 5).getClosePrice();
        return curr.isGreaterThan(threshold) && curr.isGreaterThan(prev);
    }

    @Override
    public int getUnstableBars() {
        return 5;
    }
}
