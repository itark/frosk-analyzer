package nu.itark.frosk.strategies.rules;

import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Satisfied when at least {@code threshold} of the wrapped rules are satisfied
 * at the bar — a simple k-of-n vote.
 *
 * <p>Used by the ensemble meta-strategy to require that a majority of its
 * constituent strategies' entry (or exit) rules agree before it acts. Each
 * wrapped rule is evaluated with the SAME {@code tradingRecord} the ensemble
 * passes in, so position-aware constituent rules (stop/time/cooldown) reckon
 * against the ensemble's own position, not a private one.
 */
public class CountSatisfiedRule extends AbstractRule {

    private final int threshold;
    private final Rule[] rules;

    public CountSatisfiedRule(int threshold, Rule... rules) {
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be >= 1, was " + threshold);
        }
        this.threshold = threshold;
        this.rules = rules;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int count = 0;
        for (Rule rule : rules) {
            if (rule.isSatisfied(index, tradingRecord)) {
                count++;
                if (count >= threshold) {
                    break;
                }
            }
        }
        boolean satisfied = count >= threshold;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
