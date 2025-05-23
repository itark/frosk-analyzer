package nu.itark.frosk.strategies.indicators;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.service.HedgeIndexService;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * https://www.investopedia.com/terms/v/vix.asp
 * <p>
 * Part of Risk-on score
 */
@Slf4j
public class VixLowRiskIndicator extends CachedIndicator<Boolean> {
    Num threshold = DoubleNum.valueOf(25);
    HedgeIndexService hedgeIndexService;

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public VixLowRiskIndicator(BarSeries series, HedgeIndexService hedgeIndexService) {
        super(series);
        this.hedgeIndexService = hedgeIndexService;
    }

    @Override
    protected Boolean calculate(int index) {
        Bar currBar = getBarSeries().getBar(index);
        return currBar.isBearish() && currBar.getClosePrice().isLessThanOrEqual(threshold);
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
