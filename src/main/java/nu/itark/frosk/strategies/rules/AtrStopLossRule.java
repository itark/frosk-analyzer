package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * ATR-based stop-loss: satisfied when the close drops more than
 * {@code multiplier × ATR(entryIndex)} below the entry price. The stop level
 * is fixed at entry, so it scales with the volatility at the time of entry.
 */
public class AtrStopLossRule extends AbstractRule {

    private final BarSeries           series;
    private final ATRIndicator        atr;
    private final double              multiplier;
    private final ClosePriceIndicator close;

    public AtrStopLossRule(BarSeries series, int atrPeriod, double multiplier) {
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
        Num entryPrice = close.getValue(entryIndex);
        Num stopLevel  = entryPrice.minus(atr.getValue(entryIndex).multipliedBy(series.numOf(multiplier)));
        return close.getValue(index).isLessThanOrEqual(stopLevel);
    }
}
