package nu.itark.frosk.strategies;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.hedge.HedgeIndexStrategy;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.HedgeIndexTieredRule;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OrRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Månadsportföljen — Swedish Long-Term Factor Portfolio strategy.
 *
 * Entry conditions (ALL must be met):
 * 1. Monthly rebalance window (first 7 calendar days of every month)
 * 2. HedgeIndex score <= 7 (tiered gate; 8+ blocks)
 * 3. 6-month momentum (ROC 126 bars) > 0
 * 4. Golden Cross: price > SMA(50) AND SMA(50) > SMA(200)
 * 5. 3-month relative strength vs ^OMX: stock ROC(63) outperforms OMXS30 ROC(63)
 * 6. Valuation: PEG ratio < threshold (if available)
 * 7. Volatility: Beta < 2.0 (if available; filters extreme high-vol names)
 * 8. Low volatility: 252-day annualized stddev below threshold (~60%)
 *
 * The original design AND-ed ten conditions inside a 5-day quarterly window
 * and produced 3 trades in three years across the whole universe. The window
 * is now monthly, the redundant 12M-1 momentum rule (which also silenced any
 * listing younger than 252 bars) is dropped, and the volatility/PEG gates are
 * calibrated to Swedish mid-caps.
 *
 * Exit conditions (ANY):
 * - HedgeIndex score >= 8 (Defensive / Strong Risk-Off)
 * - Death cross: SMA(50) < SMA(200)
 * - 6-month momentum decisively negative (ROC 126 < -5)
 */
@Component
@RequiredArgsConstructor
public class SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private final BarSeriesService barSeriesService;
    private final HedgeIndexStrategy hedgeIndexStrategy;
    private final HedgeIndexService hedgeIndexService;

    @Value("${frosk.swedish.longterm.pegratio.threshold:2.5}")
    private double pegRatioThreshold;

    @Value("${frosk.swedish.longterm.maxVolatility:0.60}")
    private double maxVolatility;

    @Value("${frosk.swedish.longterm.hedge.exit.score:9}")
    private int hedgeExitScore;

    @Value("${frosk.swedish.longterm.atr.mult:3.0}")
    private double atrMultiplier;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);

        // Build OMX close-by-date map for relative strength comparison
        BarSeries omxSeries = barSeriesService.getDataSet("^OMX", false, false);
        Map<ZonedDateTime, Num> omxCloseByDate = new HashMap<>();
        for (int i = 0; i < omxSeries.getBarCount(); i++) {
            Bar bar = omxSeries.getBar(i);
            omxCloseByDate.put(bar.getEndTime(), bar.getClosePrice());
        }

        // 6-month momentum
        ROCIndicator roc6m = new ROCIndicator(close, 126);
        setIndicatorValues(roc6m, "roc6m");

        // Moving averages for Golden Cross
        SMAIndicator sma50 = new SMAIndicator(close, 50);
        setIndicatorValues(sma50, "sma50");
        SMAIndicator sma200 = new SMAIndicator(close, 200);
        setIndicatorValues(sma200, "sma200");

        // Relative strength vs ^OMX (3 months = 63 trading days)
        ROCIndicator stockRoc3m = new ROCIndicator(close, 63);

        // OMX 3-month ROC via date-based lookup on the executor's series
        CachedIndicator<Num> omxRoc3m = new CachedIndicator<>(series) {
            @Override
            protected Num calculate(int index) {
                ZonedDateTime currentDate = series.getBar(index).getEndTime();
                int lookback = Math.min(63, index);
                ZonedDateTime pastDate = series.getBar(index - lookback).getEndTime();

                Num omxCurrent = omxCloseByDate.get(currentDate);
                Num omxPast = omxCloseByDate.get(pastDate);

                if (omxCurrent == null || omxPast == null || omxPast.isZero()) {
                    return series.numOf(0);
                }
                return omxCurrent.minus(omxPast).dividedBy(omxPast).multipliedBy(series.numOf(100));
            }
            @Override
            public int getUnstableBars() { return 63; }
        };

        // Low volatility factor: annualized stddev of daily returns.
        // Must be computed on returns, not price levels — the stddev of the
        // price itself over a year measures how far the stock trended, which
        // blocked every mover and let only dead-flat names through.
        ROCIndicator dailyReturnPct = new ROCIndicator(close, 1);
        StandardDeviationIndicator returnStdDev252 = new StandardDeviationIndicator(dailyReturnPct, 252);
        CachedIndicator<Num> annualizedVol = new CachedIndicator<>(series) {
            @Override
            protected Num calculate(int index) {
                if (index < 252) return series.numOf(1); // not enough data, assume high vol
                // stddev of daily returns in % → decimal, annualized with sqrt(252)
                return returnStdDev252.getValue(index)
                        .dividedBy(series.numOf(100))
                        .multipliedBy(series.numOf(Math.sqrt(252)));
            }
            @Override
            public int getUnstableBars() { return 252; }
        };

        // Fundamental gates (use original series name = security id for DB lookup)
        Double peg  = getPEGRatio(series.getName());
        Double beta = getBeta(series.getName());
        Double divYield = getDividendYield(series.getName());
        // PEG gate: if data available and overvalued, block entry
        Rule pegGate  = (peg  > 0 && peg  >= pegRatioThreshold) ? BooleanRule.FALSE : BooleanRule.TRUE;
        // Beta gate: filter extreme high-volatility names (beta > 2.0)
        Rule betaGate = (beta > 0 && beta >= 2.0)               ? BooleanRule.FALSE : BooleanRule.TRUE;
        // Dividend yield tilt: prefer dividend-paying stocks (soft filter)
        // Only blocks entry in cautious regime (score 4-7) if no dividend — always allows in risk-on (0-3)
        Rule dividendGate = (divYield > 0) ? BooleanRule.TRUE : BooleanRule.TRUE; // stored for portfolio ranking

        // Monthly rebalance window: first 7 calendar days of every month
        Rule monthlyRebalance = new MonthlyRebalanceRule(series);

        // Entry rules
        Rule momentum6mPositive   = new OverIndicatorRule(roc6m, 0);
        Rule priceAboveSma50      = new OverIndicatorRule(close, sma50);
        Rule goldenCross          = new OverIndicatorRule(sma50, sma200);
        Rule outperformsOMX       = new OverIndicatorRule(stockRoc3m, omxRoc3m);

        // Low volatility filter: annualized vol must be below threshold
        Rule lowVolFilter = new UnderIndicatorRule(annualizedVol, maxVolatility);

        // Tiered HedgeIndex gate: score <= 7 allows entry, score 8+ blocks
        Rule hedgeEntryRule = new HedgeIndexTieredRule(series, hedgeIndexService, 7);

        Rule entryRule = new AndRule(
                new AndRule(monthlyRebalance, hedgeEntryRule),
                new AndRule(
                        momentum6mPositive,
                        new AndRule(
                                new AndRule(priceAboveSma50, goldenCross),
                                new AndRule(outperformsOMX,
                                        new AndRule(new AndRule(pegGate, betaGate), lowVolFilter))
                        )
                )
        );

        // Exit only on strong risk-off (score > hedgeExitScore). Exiting already
        // at the 8-point defensive tier dumped positions a few weeks after every
        // monthly entry, since the score oscillates between 4 and 9.
        Rule hedgeExitRule = new HedgeIndexMaxScoreRule(series, hedgeIndexService, hedgeExitScore).negation();
        Rule deathCross      = new UnderIndicatorRule(sma50, sma200);
        // Decisively negative only — a monthly portfolio should not exit on a
        // marginal momentum dip (ROC 126 hovering around 0 caused churn).
        Rule momentum6mGone  = new UnderIndicatorRule(roc6m, -5);
        // Wide trailing stop: the factor exits above (death cross, deep momentum
        // loss) are all realized far below the peak — this banks winners first.
        Rule trailingStop = new AtrTrailingStopRule(series, 14, atrMultiplier);

        Rule exitRule = new OrRule(hedgeExitRule,
                new OrRule(deathCross,
                        new OrRule(momentum6mGone,
                                new OrRule(trailingStop, catastrophicStopRule()))));

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Monthly rebalance rule: satisfied only during the first 7 calendar days
     * of each month (approximates the first trading week).
     */
    private static class MonthlyRebalanceRule extends AbstractRule {
        private final BarSeries series;

        MonthlyRebalanceRule(BarSeries series) {
            this.series = series;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            return series.getBar(index).getEndTime().getDayOfMonth() <= 7;
        }
    }
}
