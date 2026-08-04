package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.MedianTurnoverIndicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Satisfied only when the intended position is small relative to the instrument's
 * normal daily turnover — i.e. when the trade could realistically be filled.
 *
 * <p>Backtests fill at the bar close for any size, which silently invents liquidity
 * that does not exist. On a CANSLIM review of 61 Nordic securities, 13 of them could
 * not absorb a 20,000 SEK position within 2% of their median daily turnover, and one
 * (MTG-A.ST) turned over less per day than the position size itself — 224% of median
 * daily turnover. Those 43 trades were pure backtest fiction.
 *
 * <p>The rule is deliberately a hard gate on entry rather than a cost adjustment:
 * market impact on a name that thin is not a few basis points to subtract, it is the
 * absence of a counterparty.
 */
public class LiquidityRule extends AbstractRule {

    private final MedianTurnoverIndicator medianTurnover;
    private final Num positionValue;
    private final Num maxPctOfTurnover;

    /**
     * @param medianTurnover   median traded value per bar, in the instrument's currency
     * @param positionValue    the position size the strategy intends to take
     * @param maxPctOfTurnover the largest share of median daily turnover the position
     *                         may represent, in percent (e.g. 1.0 for 1%)
     */
    public LiquidityRule(MedianTurnoverIndicator medianTurnover, Num positionValue, Num maxPctOfTurnover) {
        this.medianTurnover = medianTurnover;
        this.positionValue = positionValue;
        this.maxPctOfTurnover = maxPctOfTurnover;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        Num turnover = medianTurnover.getValue(index);
        boolean satisfied = false;
        // Zero turnover means either no volume data or not enough history — block,
        // never assume tradability we have not observed.
        if (turnover != null && turnover.isGreaterThan(turnover.numOf(0))) {
            Num positionPct = positionValue.dividedBy(turnover).multipliedBy(turnover.numOf(100));
            satisfied = positionPct.isLessThanOrEqual(maxPctOfTurnover);
        }
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
