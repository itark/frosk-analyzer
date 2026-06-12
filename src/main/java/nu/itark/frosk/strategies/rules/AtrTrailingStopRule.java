package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * ATR trailing stop (chandelier exit): satisfied when the close falls more
 * than {@code multiplier × ATR(index)} below the highest close reached since
 * entry. Lets winners run while ratcheting the stop up behind them.
 */
public class AtrTrailingStopRule extends AbstractRule {

    private final BarSeries           series;
    private final ATRIndicator        atr;
    private final double              multiplier;
    private final ClosePriceIndicator close;

    public AtrTrailingStopRule(BarSeries series, int atrPeriod, double multiplier) {
        this.series     = series;
        this.atr        = new ATRIndicator(series, atrPeriod);
        this.multiplier = multiplier;
        this.close      = new ClosePriceIndicator(series);
    }

    /** This rule uses the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        Num highestSinceEntry = close.getValue(entryIndex);
        for (int i = entryIndex + 1; i <= index; i++) {
            Num value = close.getValue(i);
            if (value.isGreaterThan(highestSinceEntry)) {
                highestSinceEntry = value;
            }
        }
        Num stopLevel = highestSinceEntry.minus(atr.getValue(index).multipliedBy(series.numOf(multiplier)));
        return close.getValue(index).isLessThanOrEqual(stopLevel);
    }
}
