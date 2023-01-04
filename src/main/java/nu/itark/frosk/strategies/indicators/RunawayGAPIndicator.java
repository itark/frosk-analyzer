package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * https://www.investopedia.com/terms/b/runawaygap.asp
 */
public class RunawayGAPIndicator extends CachedIndicator<Boolean> {

    /**
     * Constructor.
     *
     * @param series a bar series
     */
    public RunawayGAPIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(int index) {
        if (index < 1) {
            // GAP is a 2-candle pattern
            return false;
        }
        Bar prevBar = getBarSeries().getBar(index - 1);
        Bar currBar = getBarSeries().getBar(index);
        if (currBar.isBullish()) {
            final Num prevHighPrice = prevBar.getHighPrice();
            final Num currLowPrice = currBar.getLowPrice();
            return currLowPrice.isGreaterThan(prevHighPrice);
        }
        return false;
    }


}
