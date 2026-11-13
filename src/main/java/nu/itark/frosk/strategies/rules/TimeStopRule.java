package nu.itark.frosk.strategies.rules;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Time-stop with a profit condition — S10 in
 * {@code docs/research/omx-equity-strategies-research.md}.
 *
 * <p>Satisfied once the current (long) position has been held for at least {@code maxBars}
 * bars <em>and</em> its unrealized gain is still below {@code minProfitPct} percent. The idea
 * (Sperandeo; systematic time-exit studies): capital tied up in a stock that has not performed
 * within the holding window is capital that is not working — free it for better opportunities.
 *
 * <p>A position that has already reached the profit target is left alone, so this never forces
 * an exit on a working trade — other exit rules (trailing/stop/regime) manage winners.
 *
 * <p>Related to {@link MaxBarsHeldRule} (pure time), of which this is the profit-aware form.
 */
public class TimeStopRule extends AbstractRule {

    private final Indicator<Num> price;
    private final int maxBars;
    private final double minProfitPct;

    public TimeStopRule(Indicator<Num> price, int maxBars, double minProfitPct) {
        this.price = price;
        this.maxBars = maxBars;
        this.minProfitPct = minProfitPct;
    }

    /** This rule uses the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        if ((index - entryIndex) < maxBars) {
            return false;
        }
        Num entryPrice = tradingRecord.getCurrentPosition().getEntry().getNetPrice();
        if (entryPrice == null || entryPrice.isZero()) {
            return false;
        }
        Num current = price.getValue(index);
        double gainPct = current.minus(entryPrice).dividedBy(entryPrice).doubleValue() * 100.0;
        boolean satisfied = gainPct < minProfitPct;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
