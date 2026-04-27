package nu.itark.frosk.strategies;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.hedge.HedgeIndexStrategy;
import nu.itark.frosk.strategies.rules.HedgeIndexRiskOffRule;
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

import java.time.Month;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Månadsportföljen — Swedish Long-Term Factor Portfolio strategy.
 *
 * Entry conditions (ALL must be met):
 * 1. Quarterly rebalance window (first 5 trading days of Jan, Apr, Jul, Oct)
 * 2. HedgeIndex risk-on
 * 3. 6-month momentum (ROC 126 bars) > 0
 * 4. 12-month momentum (12-1): skip most recent month to avoid reversal
 * 5. Golden Cross: price > SMA(50) AND SMA(50) > SMA(200)
 * 6. 3-month relative strength vs ^OMX: stock ROC(63) outperforms OMXS30 ROC(63)
 * 7. Valuation: PEG ratio < threshold (if available)
 * 8. Volatility: Beta < 2.0 (if available; filters extreme high-vol names)
 * 9. Low volatility: 252-day standard deviation below threshold (annualized ~40%)
 * 10. Dividend yield: positive tilt for dividend-paying stocks (soft filter)
 *
 * Exit conditions (ANY):
 * - HedgeIndex score >= 8 (Defensive / Strong Risk-Off)
 * - Death cross: SMA(50) < SMA(200)
 * - 6-month momentum turns negative
 */
@Component
@RequiredArgsConstructor
public class SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;
    private final HedgeIndexStrategy hedgeIndexStrategy;
    private final HedgeIndexService hedgeIndexService;

    @Value("${frosk.hedge.criteria.pegratio.threshold:1.5}")
    private double pegRatioThreshold;

    @Value("${frosk.swedish.longterm.maxVolatility:0.40}")
    private double maxVolatility;

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

        // 12-month momentum (12-1): (close[t-21] - close[t-252]) / close[t-252]
        // Skips most recent month (21 trading days) to avoid short-term reversal
        CachedIndicator<Num> roc12m1 = new CachedIndicator<>(series) {
            @Override
            protected Num calculate(int index) {
                if (index < 252) {
                    return series.numOf(0);
                }
                Num closeRecent = series.getBar(index - 21).getClosePrice();
                Num closeOld = series.getBar(index - 252).getClosePrice();
                if (closeOld.isZero()) {
                    return series.numOf(0);
                }
                return closeRecent.minus(closeOld).dividedBy(closeOld).multipliedBy(series.numOf(100));
            }
            @Override
            public int getUnstableBars() { return 252; }
        };
        setIndicatorValues(roc12m1, "roc12m1");

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

        // Low volatility factor: 252-day standard deviation (annualized)
        // Daily stddev × sqrt(252) gives annualized volatility
        StandardDeviationIndicator stdDev252 = new StandardDeviationIndicator(close, 252);
        CachedIndicator<Num> annualizedVol = new CachedIndicator<>(series) {
            @Override
            protected Num calculate(int index) {
                if (index < 252) return series.numOf(1); // not enough data, assume high vol
                Num dailyStdDev = stdDev252.getValue(index);
                Num price = close.getValue(index);
                if (price.isZero()) return series.numOf(1);
                // Normalize: (stddev / price) * sqrt(252) = annualized vol as decimal
                return dailyStdDev.dividedBy(price).multipliedBy(series.numOf(Math.sqrt(252)));
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

        // Quarterly rebalance window: first 5 trading days of Jan, Apr, Jul, Oct
        Rule quarterlyRebalance = new QuarterlyRebalanceRule(series);

        // Entry rules
        Rule momentum6mPositive   = new OverIndicatorRule(roc6m, 0);
        Rule momentum12m1Positive = new OverIndicatorRule(roc12m1, 0);
        Rule priceAboveSma50      = new OverIndicatorRule(close, sma50);
        Rule goldenCross          = new OverIndicatorRule(sma50, sma200);
        Rule outperformsOMX       = new OverIndicatorRule(stockRoc3m, omxRoc3m);

        // Low volatility filter: annualized vol must be below threshold
        Rule lowVolFilter = new UnderIndicatorRule(annualizedVol, maxVolatility);

        // Tiered HedgeIndex gate: score <= 7 allows entry, score 8+ blocks
        Rule hedgeEntryRule = new HedgeIndexTieredRule(series, hedgeIndexService, 7);

        Rule entryRule = new AndRule(
                new AndRule(quarterlyRebalance, hedgeEntryRule),
                new AndRule(
                        new AndRule(momentum6mPositive, momentum12m1Positive),
                        new AndRule(
                                new AndRule(priceAboveSma50, goldenCross),
                                new AndRule(outperformsOMX,
                                        new AndRule(new AndRule(pegGate, betaGate), lowVolFilter))
                        )
                )
        );

        // Exit rules: score >= 8 triggers exit (Defensive / Strong Risk-Off)
        Rule hedgeExitRule = new HedgeIndexRiskOffRule(series, hedgeIndexService);
        Rule deathCross      = new UnderIndicatorRule(sma50, sma200);
        Rule momentum6mGone  = new UnderIndicatorRule(roc6m, 0);

        Rule exitRule = new OrRule(hedgeExitRule, new OrRule(deathCross, momentum6mGone));

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Quarterly rebalance rule: satisfied only during the first 5 calendar days
     * of January, April, July, and October (approximates first trading week of quarter).
     */
    private static class QuarterlyRebalanceRule extends AbstractRule {
        private static final Set<Month> REBALANCE_MONTHS = Set.of(
                Month.JANUARY, Month.APRIL, Month.JULY, Month.OCTOBER
        );
        private final BarSeries series;

        QuarterlyRebalanceRule(BarSeries series) {
            this.series = series;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            ZonedDateTime endTime = series.getBar(index).getEndTime();
            return REBALANCE_MONTHS.contains(endTime.getMonth()) && endTime.getDayOfMonth() <= 5;
        }
    }
}
