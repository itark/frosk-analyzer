package nu.itark.frosk.strategies.indicators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.HedgeIndex;
import nu.itark.frosk.repo.HedgeIndexRepository;
import nu.itark.frosk.service.HedgeIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
public class VixRiskOnIndicator extends CachedIndicator<Boolean> {
    Num threshold = DoubleNum.valueOf(25);
    HedgeIndexService hedgeIndexService;

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public VixRiskOnIndicator(BarSeries series, HedgeIndexService hedgeIndexService) {
        super(series);
        this.hedgeIndexService = hedgeIndexService;
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < 1) {
            // GAP is a 2-candle pattern
            return false;
        }
        Bar currBar = getBarSeries().getBar(index);
        if (currBar.isBearish()) {  //TODO:  could introduce lagg
            return currBar.getClosePrice().isLessThanOrEqual(threshold);
        }
        return false;
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
