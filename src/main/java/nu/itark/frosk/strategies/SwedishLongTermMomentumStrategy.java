package nu.itark.frosk.strategies;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.hedge.BarSeriesAligner;
import nu.itark.frosk.strategies.hedge.HedgeIndexStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OrRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Månadsportföljen — Swedish Long-Term Factor Portfolio strategy.
 *
 * Entry conditions (ALL must be met):
 * 1. HedgeIndex risk-on
 * 2. 6-month momentum (ROC 126 bars) > 0
 * 3. 12-month momentum (ROC 252 bars) > 0
 * 4. Golden Cross: price > SMA(50) AND SMA(50) > SMA(200)
 * 5. 3-month relative strength vs ^OMX: stock ROC(63) outperforms OMXS30 ROC(63)
 * 6. Valuation: PEG ratio < threshold (if available)
 * 7. Volatility: Beta < 2.0 (if available; filters extreme high-vol names)
 *
 * Exit conditions (ANY):
 * - HedgeIndex turns risk-off
 * - Death cross: SMA(50) < SMA(200)
 * - 6-month momentum turns negative
 */
@Component
@RequiredArgsConstructor
public class SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;
    private final HedgeIndexStrategy hedgeIndexStrategy;

    @Value("${frosk.hedge.criteria.pegratio.threshold:1.5}")
    private double pegRatioThreshold;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        // Align with ^OMX for 3-month relative-strength comparison
        BarSeries omxSeries = barSeriesService.getDataSet("^OMX", false, false);
        List<BarSeries> aligned = BarSeriesAligner.alignAndTruncate(
                List.of(series, omxSeries), series.getBarCount());
        BarSeries alignedStock = aligned.get(0);
        BarSeries alignedOMX = aligned.get(1);

        super.barSeries = alignedStock;

        ClosePriceIndicator close = new ClosePriceIndicator(alignedStock);
        ClosePriceIndicator omxClose = new ClosePriceIndicator(alignedOMX);

        // Momentum indicators
        ROCIndicator roc6m = new ROCIndicator(close, 126);
        setIndicatorValues(roc6m, "roc6m");
        ROCIndicator roc12m = new ROCIndicator(close, 252);
        setIndicatorValues(roc12m, "roc12m");

        // Moving averages for Golden Cross
        SMAIndicator sma50 = new SMAIndicator(close, 50);
        setIndicatorValues(sma50, "sma50");
        SMAIndicator sma200 = new SMAIndicator(close, 200);
        setIndicatorValues(sma200, "sma200");

        // Relative strength vs ^OMX (3 months = 63 trading days)
        ROCIndicator stockRoc3m = new ROCIndicator(close, 63);
        ROCIndicator omxRoc3m = new ROCIndicator(omxClose, 63);

        // Fundamental gates (use original series name = security id for DB lookup)
        Double peg  = getPEGRatio(series.getName());
        Double beta = getBeta(series.getName());
        // PEG gate: if data available and overvalued, block entry
        Rule pegGate  = (peg  > 0 && peg  >= pegRatioThreshold) ? BooleanRule.FALSE : BooleanRule.TRUE;
        // Beta gate: filter extreme high-volatility names (beta > 2.0)
        Rule betaGate = (beta > 0 && beta >= 2.0)               ? BooleanRule.FALSE : BooleanRule.TRUE;

        // Entry rules
        Rule momentum6mPositive  = new OverIndicatorRule(roc6m, 0);
        Rule momentum12mPositive = new OverIndicatorRule(roc12m, 0);
        Rule priceAboveSma50     = new OverIndicatorRule(close, sma50);
        Rule goldenCross         = new OverIndicatorRule(sma50, sma200);
        Rule outperformsOMX      = new OverIndicatorRule(stockRoc3m, omxRoc3m);

        Strategy hedge = hedgeIndexStrategy.buildStrategy(alignedStock);

        Rule entryRule = new AndRule(
                new AndRule(hedge.getEntryRule(), new AndRule(momentum6mPositive, momentum12mPositive)),
                new AndRule(new AndRule(priceAboveSma50, goldenCross),
                        new AndRule(outperformsOMX, new AndRule(pegGate, betaGate)))
        );

        // Exit rules
        Rule deathCross      = new UnderIndicatorRule(sma50, sma200);
        Rule momentum6mGone  = new UnderIndicatorRule(roc6m, 0);

        Rule exitRule = new OrRule(hedge.getExitRule(), new OrRule(deathCross, momentum6mGone));

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
