package nu.itark.frosk.strategies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.hedge.GoldenCrossRelativeStrengthStrategy;
import nu.itark.frosk.strategies.hedge.HedgeIndexStrategy;
import nu.itark.frosk.strategies.hedge.YoYRevenueGrowthStrategy;
import nu.itark.frosk.strategies.indicators.HighestValueIndicator;
import nu.itark.frosk.strategies.indicators.MultipliedIndicator;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * CANSLIM Strategy — William O'Neil's growth stock selection framework,
 * adapted for OMXS30 components using available fundamental and technical data.
 *
 * Implemented criteria:
 *   C — Current Earnings   : trailingEps > 0 (proxy: company is profitable)
 *   A — Annual Earnings    : yoyGrowth > threshold (via YoYRevenueGrowthStrategy)
 *   N — New / Price Leader : close > 95% of 52-week high (price leadership)
 *   S — Supply & Demand    : volume > 1.5× SMA(volume, 20) (institutional accumulation)
 *   L — Leader not Laggard : price > SMA50 > SMA200, Golden Cross confirmed
 *   M — Market Direction   : HedgeIndex risk-on (macro regime gate)
 *
 * Omitted:
 *   I — Institutional Sponsorship: no ownership data available in current data model
 *
 * Exit: price < SMA50 OR Death Cross OR HedgeIndex risk-off OR hard stop-loss
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CANSLIMStrategy extends AbstractStrategy implements IIndicatorValue {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private final HedgeIndexStrategy hedgeIndexStrategy;
    private final GoldenCrossRelativeStrengthStrategy goldenCrossRelativeStrengthStrategy;
    private final YoYRevenueGrowthStrategy yoYRevenueGrowthStrategy;

    @Value("${frosk.canslim.stoploss.percent:15.0}")
    private double stopLossPercent;

    @Value("${frosk.canslim.volume.multiplier:1.5}")
    private double volumeMultiplier;

    @Value("${frosk.canslim.fiftytwo.week.threshold:0.95}")
    private double fiftyTwoWeekThreshold;

    private static final int VOLUME_SMA_PERIOD = 20;
    private static final int FIFTY_TWO_WEEK_BARS = 252;

    /**
     * Builds a CANSLIM composite strategy gated by six criteria.
     *
     * Entry: M AND L AND A AND C AND S AND N
     * Exit:  price < SMA50 OR Death Cross OR HedgeIndex exit OR hard stop-loss
     */
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma50  = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);

        setIndicatorValues(closePrice, "close");
        setIndicatorValues(sma50, "sma50");
        setIndicatorValues(sma200, "sma200");

        // --- M: Market Direction — HedgeIndex must be risk-on ---
        Strategy hedgeStrategy = hedgeIndexStrategy.buildStrategy(series);
        Rule marketDirectionRule = hedgeStrategy.getEntryRule();

        // --- L: Leader — price > SMA50 > SMA200 with confirmed Golden Cross ---
        Strategy goldenCrossStrategy = goldenCrossRelativeStrengthStrategy.buildStrictGoldenCrossStrategy(series);
        Rule leaderRule = goldenCrossStrategy.getEntryRule();

        // --- A: Annual Earnings — yoyGrowth > frosk.hedge.criteria.yoygrowth.threshold ---
        Strategy yoyStrategy = yoYRevenueGrowthStrategy.buildStrategy(series);
        Rule annualGrowthRule = yoyStrategy.getEntryRule();

        // --- C: Current Earnings — trailingEps > 0 (company is profitable) ---
        Double trailingEps = getTrailingEps(series.getName());
        Rule epsPositiveRule = new BooleanRule(trailingEps != null && trailingEps > 0);
        log.debug("CANSLIM [{}] trailingEps={}", series.getName(), trailingEps);

        // --- S: Supply & Demand — volume > volumeMultiplier × SMA(volume, 20) ---
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, VOLUME_SMA_PERIOD);
        MultipliedIndicator volumeThreshold = new MultipliedIndicator(avgVolume, series.numOf(volumeMultiplier));
        Rule volumeSurgeRule = new OverIndicatorRule(volume, volumeThreshold);

        // --- N: New / Price Leadership — close > fiftyTwoWeekThreshold × 52-week high ---
        HighestValueIndicator fiftyTwoWeekHigh = new HighestValueIndicator(closePrice, FIFTY_TWO_WEEK_BARS);
        MultipliedIndicator highThreshold = new MultipliedIndicator(fiftyTwoWeekHigh, series.numOf(fiftyTwoWeekThreshold));
        Rule nearNewHighRule = new OverIndicatorRule(closePrice, highThreshold);

        // --- Composite entry: ALL six criteria must be satisfied ---
        Rule entryRule = marketDirectionRule
                .and(leaderRule)
                .and(annualGrowthRule)
                .and(epsPositiveRule)
                .and(volumeSurgeRule)
                .and(nearNewHighRule);

        // --- Exit: any one of four conditions triggers ---
        Rule stopLoss        = new StopLossRule(closePrice, stopLossPercent);
        Rule priceBelowSma50 = new UnderIndicatorRule(closePrice, sma50);
        Rule deathCross      = new UnderIndicatorRule(sma50, sma200);
        Rule hedgeExit       = hedgeStrategy.getExitRule();

        Rule exitRule = priceBelowSma50
                .or(deathCross)
                .or(hedgeExit)
                .or(stopLoss);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
