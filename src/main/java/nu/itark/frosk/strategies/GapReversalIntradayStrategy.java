package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.DayLowIndicator;
import nu.itark.frosk.strategies.indicators.GapPercentIndicator;
import nu.itark.frosk.strategies.indicators.IntradayBarCountIndicator;
import nu.itark.frosk.strategies.indicators.PreviousDayCloseIndicator;
import nu.itark.frosk.strategies.rules.AtrStopLossRule;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Gap Reversal Morning Trade — "Morgonfällan"
 *
 * <p>When an OMX30 stock opens with a gap-down &gt; 0.8% without a fundamental news event,
 * it fills the gap the same day in 60–70% of cases. Domestic buyers absorb the overnight
 * sell flow during the morning session.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Gap-down &gt; 0.5% (today's open vs. previous day's close)</li>
 *   <li>Gap-down &lt; 3.5% (larger gaps often signal real news — don't fade those)</li>
 *   <li>After bar 1 (wait 15 min for dust to settle)</li>
 *   <li>No new day-low on current bar (support is holding = "morning low holds")</li>
 *   <li>No entry after bar 12 (~12:00)</li>
 *   <li>HedgeIndex score &le; frosk.intraday.hedge.max.score (default 9) — gap-downs during strong risk-off do not fill</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: price reaches previous day's close (gap fill complete)</li>
 *   <li>Stop: 1×ATR(14) below entry with a 0.6% floor — a fixed 0.4% stop sat
 *       inside the noise of a stock that just gapped 1–3% and caused premature
 *       stop-outs</li>
 *   <li>Max 20 bars held (~5 hours) — close by end of day</li>
 * </ul>
 *
 * <h3>Risk/reward design</h3>
 * <p>Risk: ~0.6–1% volatility-scaled stop. Reward: 0.5–3.5% gap fill.
 * Typical R:R is 1.5:1 to 4:1 with a 55–65% historical fill rate.
 */
@Component
@Slf4j
public class GapReversalIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    // ── Parameters ─────────────────────────────────────────────────────────
    private static final double GAP_MIN_PCT       = -3.5; // Max gap-down (more negative = larger gap)
    private static final double GAP_MAX_PCT       = -0.5; // Min gap-down to trigger
    private static final int    MIN_BAR_FOR_ENTRY = 1;    // Wait 1 bar (15 min) after open
    private static final int    MAX_BAR_FOR_ENTRY = 12;   // No entries after ~12:00
    private static final int    ATR_PERIOD        = 14;   // Volatility-scaled stop
    private static final double ATR_STOP_MULT     = 1.0;  // 1×ATR below entry
    private static final double STOP_FLOOR_PCT    = 0.6;  // Minimum stop distance
    private static final int    MAX_BARS_HELD     = 20;   // ~5 hours, close by EOD
    @Autowired
    private HedgeIndexService hedgeIndexService;

    /** Block entries when the HedgeIndex score exceeds this (9 = strong risk-off only). */
    @org.springframework.beans.factory.annotation.Value("${frosk.intraday.hedge.max.score:9}")
    private int intradayHedgeMaxScore;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        GapPercentIndicator gapPct = new GapPercentIndicator(series);
        PreviousDayCloseIndicator prevClose = new PreviousDayCloseIndicator(series);
        DayLowIndicator dayLow = new DayLowIndicator(series);
        IntradayBarCountIndicator barCount = new IntradayBarCountIndicator(series);

        setIndicatorValues(close, "close");
        setIndicatorValues(gapPct, "gapPct");
        setIndicatorValues(prevClose, "prevClose");

        // ── Entry ─────────────────────────────────────────────────────────
        // Gap-down between -3.0% and -0.8%
        Rule gapDown = new UnderIndicatorRule(gapPct, DoubleNum.valueOf(GAP_MAX_PCT))
                .and(new OverIndicatorRule(gapPct, DoubleNum.valueOf(GAP_MIN_PCT)));

        // Wait at least 1 bar after open
        Rule afterFirstBar = new OverIndicatorRule(barCount, DoubleNum.valueOf(MIN_BAR_FOR_ENTRY - 1));

        // Not too late — only enter in the morning
        Rule notTooLate = new UnderIndicatorRule(barCount, DoubleNum.valueOf(MAX_BAR_FOR_ENTRY + 1));

        // No new day-low on current bar (support holds)
        Rule supportHolds = new NoNewDayLowRule(close, dayLow);

        // Macro regime gate
        Rule riskOn = new HedgeIndexMaxScoreRule(series, hedgeIndexService, intradayHedgeMaxScore);

        Rule entryRule = gapDown.and(afterFirstBar).and(notTooLate).and(supportHolds).and(riskOn);

        // ── Exit ──────────────────────────────────────────────────────────
        // Profit target: price reaches previous day's close (gap fill)
        Rule gapFill = new OverIndicatorRule(close, prevClose);
        // Volatility-scaled stop with a floor: fires only when the loss exceeds
        // BOTH 1×ATR and 0.6% — i.e. effective stop distance = max(ATR, 0.6%)
        Rule stopLoss = new AtrStopLossRule(series, ATR_PERIOD, ATR_STOP_MULT)
                .and(new StopLossRule(close, STOP_FLOOR_PCT));
        // Time exit
        Rule timeExit = new MaxBarsHeldRule(MAX_BARS_HELD);

        Rule exitRule = gapFill.or(stopLoss).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    // ── Inner rule: no new day-low (support is holding) ───────────────────

    /**
     * Satisfied when the current bar's low is NOT a new day-low.
     * In other words: the low of the current bar is greater than or equal to
     * the lowest low seen on previous bars today.
     */
    private static class NoNewDayLowRule extends AbstractRule {
        private final ClosePriceIndicator close;
        private final DayLowIndicator dayLow;

        NoNewDayLowRule(ClosePriceIndicator close, DayLowIndicator dayLow) {
            this.close = close;
            this.dayLow = dayLow;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            // Current bar's low price
            var currentLow = close.getBarSeries().getBar(index).getLowPrice();
            // Running day low (includes current bar)
            var runningDayLow = dayLow.getValue(index);
            // If current low equals the running day low, this bar made a new low → not satisfied
            // If current low is above the running day low, support holds → satisfied
            return currentLow.isGreaterThan(runningDayLow);
        }
    }

}
