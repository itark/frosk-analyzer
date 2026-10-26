package nu.itark.frosk.strategies.rules;

import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Satisfied at bar {@code index} when the wrapped rule was satisfied on ANY bar
 * in the trailing window {@code [index − window + 1, index]}.
 *
 * <p>Turns a momentary trigger (an EMA cross, a one-bar stretch) into a
 * short-lived "recently fired" state, so an ensemble can count agreement across
 * signals that rarely trigger on the exact same bar. A plain same-bar k-of-n
 * vote over sparse trigger rules almost never fires (entry-bar co-occurrence
 * ≈ 0); a recency window of a few bars makes genuine near-simultaneous
 * agreement detectable without collapsing to a trivial union.
 *
 * <p>The wrapped rule is evaluated with the same {@code tradingRecord} passed
 * in, so position-aware legs reckon against the caller's position.
 */
public class RecentlySatisfiedRule extends AbstractRule {

    private final Rule base;
    private final int window;

    public RecentlySatisfiedRule(Rule base, int window) {
        if (window < 1) {
            throw new IllegalArgumentException("window must be >= 1, was " + window);
        }
        this.base = base;
        this.window = window;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = false;
        int from = Math.max(0, index - window + 1);
        for (int i = index; i >= from; i--) {
            if (base.isSatisfied(i, tradingRecord)) {
                satisfied = true;
                break;
            }
        }
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
