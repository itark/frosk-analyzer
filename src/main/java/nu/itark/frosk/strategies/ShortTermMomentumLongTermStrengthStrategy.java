package nu.itark.frosk.strategies;


import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.rules.HedgeIndexRiskOffRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OrRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Short-Term Momentum with Long-Term Strength Strategy
 *
 * Entry Conditions:
 * - Price is above both 50D and 100D SMA (Long-term strength)
 * - 10D SMA is above 20D SMA (Short-term momentum confirmation)
 *
 * Exit Conditions:
 * - Price falls below 50D SMA (loss of medium-term strength)
 * - OR 10D SMA falls below 20D SMA (loss of short-term momentum)
 * - OR HedgeIndex turns risk-off (macro regime guard)
 * - OR price drops more than stopLossPercent below entry (hard stop-loss)
 */
@Component
public class ShortTermMomentumLongTermStrengthStrategy extends AbstractStrategy implements IIndicatorValue {

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Value("${frosk.slms.stoploss.percent:15.0}")
    private double stopLossPercent;

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

        // Entry Rules
        Rule priceAbove50D = new OverIndicatorRule(closePrice, sma50);
        Rule priceAbove100D = new OverIndicatorRule(closePrice, sma100);
        Rule shortTermMomentum = new OverIndicatorRule(sma10, sma20);

        // All conditions must be met for entry
        Rule entryRule = new AndRule(
                new AndRule(priceAbove50D, priceAbove100D),
                shortTermMomentum
        );

        // Exit Rules
        Rule priceBelow50D = new UnderIndicatorRule(closePrice, sma50);
        Rule shortTermMomentumLoss = new UnderIndicatorRule(sma10, sma20);
        Rule hedgeIndexExit = new HedgeIndexRiskOffRule(series, hedgeIndexService);
        Rule stopLoss = new StopLossRule(closePrice, stopLossPercent);

        // Exit when losing medium-term strength, short-term momentum, macro turns risk-off, or hard stop hit
        Rule exitRule = priceBelow50D.or(shortTermMomentumLoss).or(hedgeIndexExit).or(stopLoss);

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
