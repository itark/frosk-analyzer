package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.HighestValueIndicator;
import nu.itark.frosk.strategies.rules.HedgeIndexRiskOffRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Dagstrategin Sub-strategy A: Daily Breakout Momentum
 *
 * Captures multi-day momentum moves when an OMXS30 stock breaks out of
 * a consolidation range on elevated volume.
 *
 * Entry: daily close > 20-day high (prior bars) AND volume > 1.5× 20-bar avg volume
 * Exit:  close < SMA(200) (death cross) OR HedgeIndex score ≥ 8 (macro risk-off)
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

        // Long-term trend filter
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        setIndicatorValues(sma200, "sma200");

        // 20-day high of previous bars: HighestValueIndicator includes today,
        // so wrap in PreviousValueIndicator to get yesterday's 20-day high.
        HighestValueIndicator high20d = new HighestValueIndicator(closePrice, 20);
        PreviousValueIndicator prevHigh20d = new PreviousValueIndicator(high20d);

        // Volume: current bar vs 1.5× 20-bar moving average
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator smaVolume20 = new SMAIndicator(volume, 20);
        TransformIndicator volThreshold = TransformIndicator.multiply(smaVolume20, 1.5);

        // Entry: breakout above 20-day high AND elevated volume
        Rule breakoutRule  = new OverIndicatorRule(closePrice, prevHigh20d);
        Rule highVolumeRule = new OverIndicatorRule(volume, volThreshold);
        Rule entryRule = breakoutRule.and(highVolumeRule);

        // Exit: death cross (close < SMA200) OR HedgeIndex macro risk-off
        Rule deathCross    = new UnderIndicatorRule(closePrice, sma200);
        Rule hedgeRiskOff  = new HedgeIndexRiskOffRule(series, hedgeIndexService);
        Rule exitRule = deathCross.or(hedgeRiskOff);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
