package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.GapPercentIndicator;
import nu.itark.frosk.strategies.indicators.IntradayBarCountIndicator;
import nu.itark.frosk.strategies.indicators.OpeningRangeHighIndicator;
import nu.itark.frosk.strategies.indicators.OpeningRangeLowIndicator;
import nu.itark.frosk.strategies.indicators.OpeningRangeWidthIndicator;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Opening Range Breakout (ORB) — "Frukosthandeln"
 *
 * <p>The most reliable intraday setup on the Stockholm exchange. Measures the
 * high/low during the first 30 minutes (09:00–09:30, = 2 bars on 15m charts),
 * then enters on a breakout above the OR high.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Close crosses above Opening Range high — a fresh breakout event, so a
 *       stopped-out position is not re-entered on the next bar above the level</li>
 *   <li>At least 2 bars into the day (OR period complete)</li>
 *   <li>OR width between 0.3% and 1.5% (volatility filter)</li>
 *   <li>Overnight gap within ±1.5% — a large gap invalidates the OR as a
 *       consolidation range</li>
 *   <li>EMA(20) rising context — close &gt; EMA(20) as trend filter</li>
 *   <li>No new entries after bar 12 (~12:00) — late breakouts mostly hit the
 *       time stop near the close</li>
 *   <li>HedgeIndex score &le; frosk.intraday.hedge.max.score (default 9) — no entries in strong risk-off</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: 1.5x the OR width above entry</li>
 *   <li>Stop loss: Close below OR low</li>
 *   <li>Max 16 bars held (~4 hours) — time-based intraday exit</li>
 *   <li>Catastrophic stop: 0.8% hard stop</li>
 * </ul>
 *
 * <h3>Risk/reward design</h3>
 * <p>Risk = OR width (entry to OR low). Reward = 1.5x OR width.
 * Typical R:R is 1:1.5. With 55–60% win rate on OMX30, this is positive expectancy.
 */
@Component
@Slf4j
public class OpeningRangeBreakoutIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {

    // ── Parameters ─────────────────────────────────────────────────────────
    private static final int    OR_BARS          = 2;    // 30 min on 15m bars
    private static final double OR_WIDTH_MIN_PCT = 0.3;  // Minimum OR width to trade
    private static final double OR_WIDTH_MAX_PCT = 1.5;  // Maximum OR width to trade
    private static final int    MIN_BAR_FOR_ENTRY = 2;   // Don't enter during OR period
    private static final int    MAX_BAR_FOR_ENTRY = 12;  // No entries after ~12:00
    private static final double GAP_LIMIT_PCT     = 1.5; // |overnight gap| must be below this
    private static final int    EMA_TREND_PERIOD  = 20;  // Trend context filter
    private static final double PROFIT_TARGET_MULTIPLIER = 1.5; // 1.5x OR width
    private static final double HARD_STOP_PCT     = 0.8; // Catastrophic stop
    private static final int    MAX_BARS_HELD     = 16;  // ~4 hours max hold
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
        OpeningRangeHighIndicator orHigh = new OpeningRangeHighIndicator(series, OR_BARS);
        OpeningRangeLowIndicator orLow = new OpeningRangeLowIndicator(series, OR_BARS);
        OpeningRangeWidthIndicator orWidth = new OpeningRangeWidthIndicator(series, OR_BARS);
        GapPercentIndicator gapPct = new GapPercentIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(close, EMA_TREND_PERIOD);
        IntradayBarCountIndicator barCount = new IntradayBarCountIndicator(series);

        setIndicatorValues(close, "close");
        setIndicatorValues(ema20, "ema20");
        setIndicatorValues(orHigh, "orHigh");
        setIndicatorValues(orLow, "orLow");

        // ── Entry ─────────────────────────────────────────────────────────
        // Close crosses above OR high (fresh breakout event, no re-entry chains)
        Rule breakout = new CrossedUpIndicatorRule(close, orHigh);
        // OR period must be complete (bar >= 2)
        Rule orComplete = new OverIndicatorRule(barCount, DoubleNum.valueOf(MIN_BAR_FOR_ENTRY - 1));
        // Not too late in the day (bar <= 12)
        Rule notTooLate = new UnderIndicatorRule(barCount, DoubleNum.valueOf(MAX_BAR_FOR_ENTRY + 1));
        // OR width between 0.3% and 1.5%
        Rule widthOk = new OverIndicatorRule(orWidth, DoubleNum.valueOf(OR_WIDTH_MIN_PCT))
                .and(new UnderIndicatorRule(orWidth, DoubleNum.valueOf(OR_WIDTH_MAX_PCT)));
        // Overnight gap within ±1.5% — a gap day invalidates the OR as a range
        Rule gapOk = new UnderIndicatorRule(gapPct, DoubleNum.valueOf(GAP_LIMIT_PCT))
                .and(new OverIndicatorRule(gapPct, DoubleNum.valueOf(-GAP_LIMIT_PCT)));
        // Trend context: close > EMA(20)
        Rule trendUp = new OverIndicatorRule(close, ema20);
        // Macro regime gate
        Rule riskOn = new HedgeIndexMaxScoreRule(series, hedgeIndexService, intradayHedgeMaxScore);

        Rule entryRule = breakout.and(orComplete).and(notTooLate).and(widthOk)
                .and(gapOk).and(trendUp).and(riskOn);

        // ── Exit ──────────────────────────────────────────────────────────
        // Profit target: price >= OR high + 1.5 * OR width
        Rule profitTarget = new ProfitTargetORRule(close, orHigh, orWidth, PROFIT_TARGET_MULTIPLIER);
        // Stop: close falls below OR low
        Rule orStop = new UnderIndicatorRule(close, orLow);
        // Time exit
        Rule timeExit = new MaxBarsHeldRule(MAX_BARS_HELD);
        // Hard stop
        Rule hardStop = new StopLossRule(close, HARD_STOP_PCT);

        Rule exitRule = profitTarget.or(orStop).or(timeExit).or(hardStop);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    // ── Inner rule: profit target based on OR width ────────────────────────

    private static class ProfitTargetORRule extends AbstractRule {
        private final ClosePriceIndicator close;
        private final OpeningRangeHighIndicator orHigh;
        private final OpeningRangeWidthIndicator orWidth;
        private final double multiplier;

        ProfitTargetORRule(ClosePriceIndicator close, OpeningRangeHighIndicator orHigh,
                           OpeningRangeWidthIndicator orWidth, double multiplier) {
            this.close = close;
            this.orHigh = orHigh;
            this.orWidth = orWidth;
            this.multiplier = multiplier;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
                return false;
            }
            // Target = OR high + multiplier * (OR high - OR low) expressed via width%
            // Simpler: target = entry price + multiplier * OR_range_in_price
            var orHighVal = orHigh.getValue(index);
            var orWidthPct = orWidth.getValue(index);
            // OR range in price = orHigh * widthPct / (100 + widthPct)
            var hundred = close.numOf(100);
            var orRange = orHighVal.multipliedBy(orWidthPct).dividedBy(hundred.plus(orWidthPct));
            var target = orHighVal.plus(orRange.multipliedBy(close.numOf(multiplier)));
            return close.getValue(index).isGreaterThanOrEqual(target);
        }
    }

}
