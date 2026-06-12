package nu.itark.frosk.strategies.rules;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Time-based exit: satisfied once the current position has been held for
 * {@code maxBars} bars or more.
 */
public class MaxBarsHeldRule extends AbstractRule {

    private final int maxBars;

    public MaxBarsHeldRule(int maxBars) {
        this.maxBars = maxBars;
    }

    /** This rule uses the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        return (index - entryIndex) >= maxBars;
    }
}
