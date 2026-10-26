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
 * <p><b>Signal bar vs. fill bar.</b> "Entry bar" above means the bar whose close
 * confirmed the reclaim (the signal bar). Under {@code TradeOnNextOpenModel} the
 * position's recorded entry index is the bar AFTER it, and that bar is in general no
 * reclaim at all — {@link SweepWindow} then finds no excursion and the stop silently
 * becomes null, leaving only target and time stop. Pass {@code entryOnNextOpen=true}
 * whenever the strategy is run under next-open so the levels are read from
 * {@code entryIndex - 1}. The time stop still counts from the actual fill.
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
    private final boolean entryOnNextOpen;

    public LiquiditySweepExitRule(BarSeries series, PreviousDayExtremeIndicator pdh,
                                  PreviousDayExtremeIndicator pdl,
                                  int confirmBars, int maxBarsHeld, boolean entryOnNextOpen) {
        this.series = series;
        this.pdh = pdh;
        this.pdl = pdl;
        this.confirmBars = confirmBars;
        this.maxBarsHeld = maxBarsHeld;
        this.entryOnNextOpen = entryOnNextOpen;
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
        int signalIndex = signalIndex(entryIndex);
        Num target = pdh.getValue(signalIndex);
        if (!target.isZero() && series.getBar(index).getHighPrice().isGreaterThanOrEqual(target)) {
            return true;                                   // target
        }
        Num stop = stopLevel(entryIndex);
        return stop != null && series.getBar(index).getLowPrice().isLessThanOrEqual(stop);
    }

    /** The reclaim bar that produced the signal — see "Signal bar vs. fill bar". */
    private int signalIndex(int entryIndex) {
        return entryOnNextOpen ? Math.max(entryIndex - 1, series.getBeginIndex()) : entryIndex;
    }

    /**
     * Stop level for a position entered at {@code entryIndex}: the lowest low across
     * the sweep excursion the signal bar terminated, or null when none is found. Uses
     * the same {@link SweepWindow} the entry rule used, so the stop can never be
     * derived from a different set of bars than the ones that produced the signal.
     */
    Num stopLevel(int entryIndex) {
        int signalIndex = signalIndex(entryIndex);
        Num pdlV = pdl.getValue(signalIndex);
        if (pdlV.isZero()) {
            return null;
        }
        return SweepWindow.sweepLow(series, signalIndex, pdlV,
                firstBarOfDay(signalIndex), confirmBars);
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
