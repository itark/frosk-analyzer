package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-off when AUD/USD drops more than 2% in the last 5 days.
 * AUD is a commodity/risk currency — sharp falls signal global risk-off appetite.
 */
@Slf4j
public class AUDUSDRiskOffIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(2);
    private final int period = 5;

    public AUDUSDRiskOffIndicator(BarSeries series) {
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
        // positive when price falls (AUD weakens)
        Num percentage = prevBar.getClosePrice().minus(currBar.getClosePrice())
                .dividedBy(prevBar.getClosePrice())
                .multipliedBy(DoubleNum.valueOf(100));
        return percentage.isGreaterThan(threshold);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
