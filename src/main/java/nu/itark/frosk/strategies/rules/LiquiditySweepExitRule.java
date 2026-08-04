package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.PreviousDayExtremeIndicator;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Exit for {@link nu.itark.frosk.strategies.CryptoLiquiditySweepIntradayStrategy}.
 *
 * <p>Implements §5 of PREREG_liquidity_sweep_15m.md. Both levels are anchored to
 * the ENTRY bar and recomputed from it on every evaluation:
 * <ul>
 *   <li><b>Stop</b> = the lowest low printed from the first penetration bar through
 *       the confirming (entry) bar — the sweep extreme</li>
 *   <li><b>Target</b> = PDH as of the entry bar</li>
 *   <li><b>Time stop</b> = {@code maxBarsHeld} bars after entry</li>
 * </ul>
 *
 * <p>Anchoring matters: PDH is a function of the evaluation bar's calendar day, so
 * a position held across UTC midnight would silently retarget to a different level
 * if the indicator were read at the current index instead of the entry index.
 *
 * <p><b>Known fidelity gap.</b> The runner polls every 15 minutes and emits a market
 * order at the bar close, so the realised fill is the CLOSE of the bar that breached
 * the level, not the level itself. Real resting stop/limit orders would fill at the
 * level. This makes live results systematically worse than a level-fill backtest —
 * the gap is largest on fast bars, exactly when stops trigger. It is a property of
 * the execution architecture, not of this rule, and must be reported as such.
 */
public class LiquiditySweepExitRule extends AbstractRule {

    private final BarSeries series;
    private final PreviousDayExtremeIndicator pdh;
    private final PreviousDayExtremeIndicator pdl;
    private final int confirmBars;
    private final int maxBarsHeld;

    public LiquiditySweepExitRule(BarSeries series, PreviousDayExtremeIndicator pdh,
                                  PreviousDayExtremeIndicator pdl,
                                  int confirmBars, int maxBarsHeld) {
        this.series = series;
        this.pdh = pdh;
        this.pdl = pdl;
        this.confirmBars = confirmBars;
        this.maxBarsHeld = maxBarsHeld;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = evaluate(index, tradingRecord);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }

    private boolean evaluate(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getCurrentPosition() == null
                || !tradingRecord.getCurrentPosition().isOpened()) {
            return false;
        }
        int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
        if (index <= entryIndex) {
            return false;
        }
        if (index - entryIndex >= maxBarsHeld) {
            return true;                                   // time stop
        }
        Num target = pdh.getValue(entryIndex);
        if (!target.isZero() && series.getBar(index).getHighPrice().isGreaterThanOrEqual(target)) {
            return true;                                   // target
        }
        Num stop = sweepLow(entryIndex);
        return stop != null && series.getBar(index).getLowPrice().isLessThanOrEqual(stop);
    }

    /**
     * Lowest low across the sweep excursion the entry bar terminated. Uses the same
     * {@link SweepWindow} the entry rule used, so the stop can never be derived from
     * a different set of bars than the ones that produced the signal.
     */
    private Num sweepLow(int entryIndex) {
        Num pdlV = pdl.getValue(entryIndex);
        if (pdlV.isZero()) {
            return null;
        }
        return SweepWindow.sweepLow(series, entryIndex, pdlV,
                firstBarOfDay(entryIndex), confirmBars);
    }

    private int firstBarOfDay(int index) {
        java.time.LocalDate day = series.getBar(index).getEndTime()
                .withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDate();
        int first = index;
        for (int i = index - 1; i >= 0; i--) {
            if (!series.getBar(i).getEndTime()
                    .withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDate().equals(day)) {
                break;
            }
            first = i;
        }
        return first;
    }
}
