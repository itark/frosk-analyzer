package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-on when NOT (10Y yield > 4.5% AND rising).
 * Complement of {@link TreasuryYield10YRiskOffIndicator}.
 */
public class TreasuryYield10YRiskOnIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(4.5);

    public TreasuryYield10YRiskOnIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < 5) return true;
        Num curr = getBarSeries().getBar(index).getClosePrice();
        Num prev = getBarSeries().getBar(index - 5).getClosePrice();
        return !(curr.isGreaterThan(threshold) && curr.isGreaterThan(prev));
    }

    @Override
    public int getUnstableBars() {
        return 5;
    }
}
