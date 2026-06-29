package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * ATR trailing stop for SHORT positions (chandelier exit — short side):
 * satisfied when the close rises more than {@code multiplier × ATR(index)}
 * above the lowest close reached since entry. Ratchets the stop DOWN as price
 * falls, protecting short-side profits.
 */
public class AtrTrailingStopShortRule extends AbstractRule {

    private final BarSeries           series;
    private final ATRIndicator        atr;
    private final double              multiplier;
    private final ClosePriceIndicator close;

    public AtrTrailingStopShortRule(BarSeries series, int atrPeriod, double multiplier) {
        this.series     = series;
        this.atr        = new ATRIndicator(series, atrPeriod);
        this.multiplier = multiplier;
        this.close      = new ClosePriceIndicator(series);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        Num lowestSinceEntry = close.getValue(entryIndex);
        for (int i = entryIndex + 1; i <= index; i++) {
            Num v = close.getValue(i);
            if (v.isLessThan(lowestSinceEntry)) {
                lowestSinceEntry = v;
            }
        }
        Num stopLevel = lowestSinceEntry.plus(atr.getValue(entryIndex).multipliedBy(series.numOf(multiplier)));
        return close.getValue(index).isGreaterThanOrEqual(stopLevel);
    }
}
