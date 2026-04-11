package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Risk-off when EUR/USD drops more than 3% in the last 10 days.
 * A sharp EUR fall signals USD flight-to-safety → equity risk-off.
 */
@Slf4j
public class EURUSDRiskOffIndicator extends CachedIndicator<Boolean> {

    private final Num threshold = DoubleNum.valueOf(3);
    private final int period = 10;

    public EURUSDRiskOffIndicator(BarSeries series) {
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
        // percentage = (prev - curr) / prev * 100 — positive when price falls
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
