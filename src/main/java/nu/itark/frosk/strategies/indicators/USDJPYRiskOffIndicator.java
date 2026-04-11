package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-off when USD/JPY rises more than 2% in the last 5 days.
 * Rapid yen weakening signals a carry trade unwind → equity risk-off.
 * Rule: 5-day ROC > +2%.
 */
@Slf4j
public class USDJPYRiskOffIndicator extends CachedIndicator<Boolean> {

    // threshold = -2: formula (prev-curr)/prev*100 is negative when price rises
    private final Num threshold = DoubleNum.valueOf(-2);
    private final int period = 5;

    public USDJPYRiskOffIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < period) {
            return false;
        }
        Bar prevBar;
        Bar currBar;
        try {
            prevBar = getBarSeries().getBar(index - period);
            currBar = getBarSeries().getBar(index);
        } catch (Exception e) {
            log.error("index:{} for name:{}", index, getBarSeries().getName());
            throw new RuntimeException(e);
        }
        // (prev - curr) / prev * 100 is negative when price rises
        Num percentage = prevBar.getClosePrice().minus(currBar.getClosePrice())
                .dividedBy(prevBar.getClosePrice())
                .multipliedBy(DoubleNum.valueOf(100));
        // risk-off: USD/JPY rose more than 2% → percentage < -2
        return percentage.isLessThan(threshold);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
