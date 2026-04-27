package nu.itark.frosk.strategies.rules;

import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Profit target rule for DailyBreakoutStrategy.
 *
 * Satisfied when close >= entry + 2 × (entry − stopLevel), where stopLevel
 * is the lowest low of the prior {@code stopLookback} bars at the moment of entry.
 */
public class BreakoutProfitTargetRule extends AbstractRule {

    private final ClosePriceIndicator closePrice;
    private final LowestValueIndicator lowestLow;
    private final Num rewardMultiplier;

    public BreakoutProfitTargetRule(ClosePriceIndicator closePrice, LowPriceIndicator lowPrice, int stopLookback) {
        this.closePrice = closePrice;
        this.lowestLow = new LowestValueIndicator(lowPrice, stopLookback);
        this.rewardMultiplier = closePrice.numOf(2);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null) {
            return false;
        }
        Position currentPosition = tradingRecord.getCurrentPosition();
        if (!currentPosition.isOpened()) {
            return false;
        }

        Num entryPrice = currentPosition.getEntry().getPricePerAsset();
        int entryIndex = currentPosition.getEntry().getIndex();
        Num stopLevel = lowestLow.getValue(entryIndex);
        Num riskPerShare = entryPrice.minus(stopLevel);

        if (riskPerShare.isNegative() || riskPerShare.isZero()) {
            return false;
        }

        Num target = entryPrice.plus(riskPerShare.multipliedBy(rewardMultiplier));
        boolean satisfied = closePrice.getValue(index).isGreaterThanOrEqual(target);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
