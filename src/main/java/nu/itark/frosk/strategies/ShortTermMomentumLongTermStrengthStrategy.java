package nu.itark.frosk.strategies;


import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Short-Term Momentum with Long-Term Strength Strategy
 *
 * Entry Conditions (all required):
 * - 10D SMA crosses above 20D SMA (fresh momentum event — not a level, so the
 *   strategy does not re-enter on every bar of an existing uptrend)
 * - Price is above both 50D and 100D SMA (long-term strength)
 * - ADX(14) above threshold (trend strength filter — skips sideways chop)
 * - HedgeIndex score at most 7 (no new entries in defensive/risk-off regimes)
 *
 * Exit Conditions (first satisfied wins):
 * - ATR trailing stop (chandelier): close falls atrMultiplier×ATR(14) below
 *   the highest close since entry — lets winners run, cuts reversals
 * - Price crosses below 50D SMA (loss of medium-term strength)
 * - HedgeIndex score above hedgeExitScore (strong risk-off only — exiting
 *   already at the 8-point defensive tier sold into weakness and hurt PnL
 *   in backtests, so the exit bar is higher than the entry gate)
 * - Hard stop-loss stopLossPercent below entry
 */
@Component
public class ShortTermMomentumLongTermStrengthStrategy extends AbstractStrategy implements IIndicatorValue {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;
    private static final int ADX_PERIOD = 14;

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Value("${frosk.slms.stoploss.percent:10.0}")
    private double stopLossPercent;

    @Value("${frosk.slms.adx.threshold:25.0}")
    private double adxThreshold;

    @Value("${frosk.slms.atr.mult:2.5}")
    private double atrMultiplier;

    @Value("${frosk.slms.hedge.max.score:7}")
    private int hedgeMaxScore;

    @Value("${frosk.slms.hedge.exit.score:9}")
    private int hedgeExitScore;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        setIndicatorValues(sma10, "sma10");
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        setIndicatorValues(sma20, "sma20");
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        setIndicatorValues(sma50, "sma50");
        SMAIndicator sma100 = new SMAIndicator(closePrice, 100);
        setIndicatorValues(sma100, "sma100");
        ADXIndicator adx = new ADXIndicator(series, ADX_PERIOD);
        setIndicatorValues(adx, "adx14");

        // Entry Rules
        Rule freshCross = new CrossedUpIndicatorRule(sma10, sma20);
        Rule priceAbove50D = new OverIndicatorRule(closePrice, sma50);
        Rule priceAbove100D = new OverIndicatorRule(closePrice, sma100);
        Rule trending = new OverIndicatorRule(adx, adxThreshold);
        Rule riskOn = new HedgeIndexMaxScoreRule(series, hedgeIndexService, hedgeMaxScore);

        Rule entryRule = freshCross
                .and(new AndRule(priceAbove50D, priceAbove100D))
                .and(trending)
                .and(riskOn);

        // Exit Rules
        Rule trailingStop = new AtrTrailingStopRule(series, ATR_PERIOD, atrMultiplier);
        Rule trendBreak = new CrossedDownIndicatorRule(closePrice, sma50);
        Rule hedgeIndexExit = new HedgeIndexMaxScoreRule(series, hedgeIndexService, hedgeExitScore).negation();
        Rule stopLoss = new StopLossRule(closePrice, stopLossPercent);

        Rule exitRule = trailingStop.or(trendBreak).or(hedgeIndexExit).or(stopLoss);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /**
     * Alternative strategy with stricter exit (both conditions required)
     */
    public Strategy buildStrictExitStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma100 = new SMAIndicator(closePrice, 100);

        // Entry Rules (same as above)
        Rule priceAbove50D = new OverIndicatorRule(closePrice, sma50);
        Rule priceAbove100D = new OverIndicatorRule(closePrice, sma100);
        Rule shortTermMomentum = new OverIndicatorRule(sma10, sma20);

        Rule entryRule = new AndRule(
                new AndRule(priceAbove50D, priceAbove100D),
                shortTermMomentum
        );

        // Strict Exit: BOTH conditions must be met
        Rule priceBelow50D = new UnderIndicatorRule(closePrice, sma50);
        Rule shortTermMomentumLoss = new UnderIndicatorRule(sma10, sma20);

        Rule strictExitRule = new AndRule(priceBelow50D, shortTermMomentumLoss);

        return new BaseStrategy(this.getClass().getSimpleName() + "_Strict", entryRule, strictExitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
