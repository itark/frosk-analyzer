package nu.itark.frosk.strategies.rules;

import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate that blocks re-entry for {@code cooldownBars} bars after a losing
 * (long) exit in the same trading record.
 *
 * <p>Measured 2026-10-07 on 1373 closed Coinbase paper trades: VWAPReversion
 * re-entries within 30 min of a losing exit in the same ticker lost -0.34 EUR/trade
 * (85 trades, -28.80 EUR) against +0.13 EUR/trade for entries with no recent loss.
 * The effect is short-lived — at a 2h window it is gone, at 8h re-entries are
 * profitable — so this is a cooldown of a few bars, never "rest of the day".
 * Applies to any losing exit, not just stop-outs: re-entries after small losing
 * time exits were the worst group (-0.48 EUR/trade).
 *
 * <p>Stateless: reads only the trading record, so it behaves identically in the
 * batch backtest and in {@code CryptoIntradayStrategyRunner}'s per-tick re-run, and
 * survives restarts. "Loss" is gross (exit price below entry price) since the
 * runner's backtest carries no cost model.
 *
 * <p>Bars are counted from the exit trade's index — under {@code TradeOnNextOpenModel}
 * that is the fill bar, one after the exit signal — so {@code cooldownBars=2} blocks
 * entries on the fill bar and the two bars after it. {@code cooldownBars <= 0}
 * disables the rule.
 */
public class CooldownAfterLossRule extends AbstractRule {

    private final int cooldownBars;

    public CooldownAfterLossRule(int cooldownBars) {
        this.cooldownBars = cooldownBars;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = evaluate(index, tradingRecord);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }

    private boolean evaluate(int index, TradingRecord tradingRecord) {
        if (cooldownBars <= 0 || tradingRecord == null) {
            return true;
        }
        Position last = tradingRecord.getLastPosition();
        if (last == null || !last.isClosed()) {
            return true;
        }
        boolean loss = last.getExit().getNetPrice().isLessThan(last.getEntry().getNetPrice());
        return !(loss && index - last.getExit().getIndex() <= cooldownBars);
    }
}
