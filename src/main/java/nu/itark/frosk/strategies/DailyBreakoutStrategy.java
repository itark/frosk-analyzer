package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.HighestValueIndicator;
import nu.itark.frosk.strategies.rules.BreakoutProfitTargetRule;
import nu.itark.frosk.strategies.rules.HedgeIndexRiskOffRule;
import nu.itark.frosk.strategies.rules.HedgeIndexTieredRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Dagstrategin Sub-strategy A: Daily Breakout Momentum
 *
 * Captures multi-day momentum moves when an OMXS30 stock breaks out of
 * a consolidation range on elevated volume.
 *
 * Precondition: close > SMA(200) — only trade breakouts in long-term uptrends
 * Entry: close > 20-day high (of high prices) by >= 0.5% AND volume > 1.5× 20-bar avg volume
 * Exit:  stop-loss at lowest low of prior 5 bars OR target at 2:1 R:R OR HedgeIndex score >= 8
 *
 * Intended universe: OMXS30 constituents (OMX30 dataset).
 * Run via StrategyAnalysis.runDagstrateginStrategies().
 */
@Component
public class DailyBreakoutStrategy extends AbstractStrategy implements IIndicatorValue {

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
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // Long-term trend filter: close > SMA(200)
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        setIndicatorValues(sma200, "sma200");

        // 20-day high of high prices (breakout reference)
        HighestValueIndicator high20d = new HighestValueIndicator(highPrice, 20);

        // Breakout threshold: close must exceed 20-day high by at least 0.5%
        TransformIndicator breakoutThreshold = TransformIndicator.multiply(high20d, 1.005);

        // Volume: current bar vs 1.5× 20-bar moving average
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator smaVolume20 = new SMAIndicator(volume, 20);
        TransformIndicator volThreshold = TransformIndicator.multiply(smaVolume20, 1.5);

        // Stop-loss level: lowest low of prior 5 bars
        org.ta4j.core.indicators.helpers.LowestValueIndicator stopLevel =
                new org.ta4j.core.indicators.helpers.LowestValueIndicator(lowPrice, 5);

        // Entry: uptrend AND breakout above 20-day high by >= 0.5% AND elevated volume
        // HedgeIndex tiered gate: score 0-3 → full, score 4-7 → allowed (sizing in portfolio), score 8+ → blocked
        Rule uptrendRule = new OverIndicatorRule(closePrice, sma200);
        Rule breakoutRule = new OverIndicatorRule(closePrice, breakoutThreshold);
        Rule highVolumeRule = new OverIndicatorRule(volume, volThreshold);
        Rule hedgeEntryGate = new HedgeIndexTieredRule(series, hedgeIndexService, 7);

        Rule entryRule = uptrendRule.and(breakoutRule).and(highVolumeRule).and(hedgeEntryGate);

        // Exit: stop-loss OR 2:1 profit target OR HedgeIndex score >= 8
        Rule stopLoss = new UnderIndicatorRule(closePrice, stopLevel);
        Rule profitTarget = new BreakoutProfitTargetRule(closePrice, lowPrice, 5);
        Rule hedgeRiskOff = new HedgeIndexRiskOffRule(series, hedgeIndexService);
        Rule exitRule = stopLoss.or(profitTarget).or(hedgeRiskOff);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
