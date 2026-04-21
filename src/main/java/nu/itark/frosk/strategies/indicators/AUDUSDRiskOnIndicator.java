package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-on when AUD/USD has NOT dropped more than 2% in the last 5 days.
 * Complement of {@link AUDUSDRiskOffIndicator}.
 */
@Slf4j
public class AUDUSDRiskOnIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(2);
    private final int period = 5;

    public AUDUSDRiskOnIndicator(BarSeries series) {
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
        Num percentage = prevBar.getClosePrice().minus(currBar.getClosePrice())
                .dividedBy(prevBar.getClosePrice())
                .multipliedBy(DoubleNum.valueOf(100));
        return !percentage.isGreaterThan(threshold);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
