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
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
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
 * Precondition: close > SMA(200) — long-term uptrend required.
 * Entry: RSI(14) < 30 AND close < lower Bollinger Band (20, 2σ)
 *        AND close > 52-week low × 1.10 AND volume > 1.2× 20-bar avg
 * Exit:  close > SMA(20) OR RSI(14) > 55 OR stop-loss (close < lowest low of 3 bars)
 *        OR HedgeIndex score >= 8
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
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // Trend filter
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        setIndicatorValues(sma200, "sma200");

        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        setIndicatorValues(sma20, "sma20");

        // RSI(14)
        RSIIndicator rsi14 = new RSIIndicator(closePrice, 14);
        setIndicatorValues(rsi14, "rsi14");

        // Bollinger Band lower (20-bar SMA - 2σ)
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(sma20);
        StandardDeviationIndicator stdDev20 = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsLowerIndicator lowerBB = new BollingerBandsLowerIndicator(bbMiddle, stdDev20);

        // 52-week low × 1.10 — filters out stocks near multi-year lows (value traps)
        LowestValueIndicator low52w = new LowestValueIndicator(closePrice, 252);
        TransformIndicator low52wFloor = TransformIndicator.multiply(low52w, 1.10);

        // Volume confirmation: volume > 1.2× 20-bar average (selling exhaustion)
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator smaVolume20 = new SMAIndicator(volume, 20);
        TransformIndicator volThreshold = TransformIndicator.multiply(smaVolume20, 1.2);

        // Stop-loss level: lowest low of prior 3 bars
        LowestValueIndicator stopLevel = new LowestValueIndicator(lowPrice, 3);

        // Entry: uptrend AND RSI < 30 AND below lower BB AND above 52w floor AND volume confirmation
        Rule uptrend       = new OverIndicatorRule(closePrice, sma200);
        Rule rsiOversold   = new UnderIndicatorRule(rsi14, 30);
        Rule belowLowerBB  = new UnderIndicatorRule(closePrice, lowerBB);
        Rule aboveLow52w   = new OverIndicatorRule(closePrice, low52wFloor);
        Rule highVolume    = new OverIndicatorRule(volume, volThreshold);

        Rule entryRule = new AndRule(
                new AndRule(uptrend, rsiOversold),
                new AndRule(belowLowerBB, new AndRule(aboveLow52w, highVolume))
        );

        // Exit: mean reversion target OR RSI recovered OR stop-loss OR macro risk-off
        Rule revertedToMean = new OverIndicatorRule(closePrice, sma20);
        Rule rsiRecovered   = new OverIndicatorRule(rsi14, 55);
        Rule stopLoss       = new UnderIndicatorRule(closePrice, stopLevel);
        Rule hedgeRiskOff   = new HedgeIndexRiskOffRule(series, hedgeIndexService);

        Rule exitRule = revertedToMean.or(rsiRecovered).or(stopLoss).or(hedgeRiskOff);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
