package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.rules.HedgeIndexRiskOffRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Dagstrategin Sub-strategy B: Daily Oversold Bounce
 *
 * Captures mean-reversion moves after sharp pullbacks in otherwise uptrending stocks.
 *
 * Precondition (merged into entry): close > SMA(200) — long-term uptrend required.
 * Entry: RSI(14) < 35 AND close < lower Bollinger Band (20, 2σ) AND close > 52-week low × 1.05
 * Exit:  close > SMA(20) (mean reversion target) OR RSI(14) > 55 OR HedgeIndex risk-off
 *
 * Intended universe: OMXS30 constituents (OMX30 dataset).
 * Run via StrategyAnalysis.runDagstrateginStrategies().
 */
@Component
public class DailyOversoldBounceStrategy extends AbstractStrategy implements IIndicatorValue {

    @Autowired
    private HedgeIndexService hedgeIndexService;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // Trend filter
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        setIndicatorValues(sma200, "sma200");

        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        setIndicatorValues(sma20, "sma20");

        // RSI(14)
        RSIIndicator rsi14 = new RSIIndicator(closePrice, 14);
        setIndicatorValues(rsi14, "rsi14");

        // Bollinger Band lower (20-bar SMA − 2σ)
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(sma20);
        StandardDeviationIndicator stdDev20 = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsLowerIndicator lowerBB = new BollingerBandsLowerIndicator(bbMiddle, stdDev20);

        // 52-week low × 1.05 — filters out stocks near multi-year lows (value traps)
        LowestValueIndicator low52w = new LowestValueIndicator(closePrice, 252);
        TransformIndicator low52wFloor = TransformIndicator.multiply(low52w, 1.05);

        // Entry: long-term uptrend AND oversold AND below lower BB AND not near 52w low
        Rule uptrend      = new OverIndicatorRule(closePrice, sma200);
        Rule rsiOversold  = new UnderIndicatorRule(rsi14, 35);
        Rule belowLowerBB = new UnderIndicatorRule(closePrice, lowerBB);
        Rule aboveLow52w  = new OverIndicatorRule(closePrice, low52wFloor);

        Rule entryRule = new AndRule(
                new AndRule(uptrend, rsiOversold),
                new AndRule(belowLowerBB, aboveLow52w)
        );

        // Exit: mean reversion target hit OR RSI recovered OR macro risk-off
        Rule revertedToMean = new OverIndicatorRule(closePrice, sma20);
        Rule rsiRecovered   = new OverIndicatorRule(rsi14, 55);
        Rule hedgeRiskOff   = new HedgeIndexRiskOffRule(series, hedgeIndexService);

        Rule exitRule = revertedToMean.or(rsiRecovered).or(hedgeRiskOff);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
