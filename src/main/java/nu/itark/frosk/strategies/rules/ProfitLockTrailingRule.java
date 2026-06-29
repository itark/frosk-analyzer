package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Two-phase trailing stop for LONG positions:
 * <ol>
 *   <li>Inactive until unrealized profit reaches {@code profitThresholdPct}.</li>
 *   <li>Once the threshold is crossed, applies a tighter chandelier trailing
 *       stop at {@code tightMultiplier × ATR(14)} below the highest close since
 *       entry — locking in gains while still letting the trade breathe.</li>
 * </ol>
 * Combine with a wider standard {@link AtrTrailingStopRule} in the exit OR so
 * that the looser stop applies before the profit threshold is reached.
 */
public class ProfitLockTrailingRule extends AbstractRule {

    private final BarSeries           series;
    private final ATRIndicator        atr;
    private final ClosePriceIndicator close;
    private final double              profitThresholdPct;
    private final double              tightMultiplier;

    public ProfitLockTrailingRule(BarSeries series, int atrPeriod,
                                   double profitThresholdPct, double tightMultiplier) {
        this.series             = series;
        this.atr                = new ATRIndicator(series, atrPeriod);
        this.close              = new ClosePriceIndicator(series);
        this.profitThresholdPct = profitThresholdPct;
        this.tightMultiplier    = tightMultiplier;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        Num entryPrice  = close.getValue(entryIndex);
        Num currentClose = close.getValue(index);

        // Only activate once the position is sufficiently in profit
        double unrealizedPct = (currentClose.doubleValue() - entryPrice.doubleValue())
                / entryPrice.doubleValue() * 100.0;
        if (unrealizedPct < profitThresholdPct) {
            return false;
        }

        // Tight chandelier: stop = highest-close-since-entry − tightMultiplier×ATR
        Num highest = entryPrice;
        for (int i = entryIndex + 1; i <= index; i++) {
            Num v = close.getValue(i);
            if (v.isGreaterThan(highest)) {
                highest = v;
            }
        }
        Num stopLevel = highest.minus(atr.getValue(index).multipliedBy(series.numOf(tightMultiplier)));
        return currentClose.isLessThanOrEqual(stopLevel);
    }
}
