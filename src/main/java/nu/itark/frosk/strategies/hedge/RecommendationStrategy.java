package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.CrossedDownIndicatorRule;
import nu.itark.frosk.strategies.indicators.CrossedUpIndicatorRule;
import nu.itark.frosk.strategies.indicators.RecommendationIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;

import java.util.List;

@Component
public class RecommendationStrategy extends AbstractStrategy implements IIndicatorValue {


/*
    public Strategy buildStrategyOLD(BarSeries series) {
        final RecommendationTrend recommendation = getRecommendation(series.getName());

        RecommendationIndicator recIndicator = new RecommendationIndicator(
                series, recommendation.getStrongBuy(), recommendation.getBuy(), recommendation.getHold(), recommendation.getSell(), recommendation.getStrongSell());

        // Buy when indicator crosses ABOVE buyThreshold
        Rule entryRule = new CrossedUpIndicatorRule(recIndicator, buyThreshold);
        // Sell when indicator crosses BELOW sellThreshold
        Rule exitRule = new CrossedDownIndicatorRule(recIndicator, sellThreshold);

        return new BaseStrategy(this.getClass().getSimpleName(),entryRule, exitRule);
    }
*/


    public Strategy buildStrategy(BarSeries series) {
        Num buyThreshold = series.numOf(2);   // Buy when score >= 2

        final RecommendationTrend recommendation = getRecommendation(series.getName());
        Rule entryRule = BooleanRule.FALSE;
        Rule exitRule = BooleanRule.TRUE;

        if (recommendation != null) {
            RecommendationIndicator recIndicator = new RecommendationIndicator(
                    series, recommendation.getStrongBuy(), recommendation.getBuy(), recommendation.getHold(), recommendation.getSell(), recommendation.getStrongSell());

            if (recIndicator.getScore().isGreaterThanOrEqual(buyThreshold)) {
                entryRule = BooleanRule.TRUE;  // Always enter (subject to additional filters if needed)
                exitRule = BooleanRule.FALSE;  // Never exit based on PEG alone
            } else {
                // PEG too high → no trade
                entryRule = BooleanRule.FALSE;
                exitRule = BooleanRule.TRUE;   // Always exit if in position
            }
        }
        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }


    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return getIndicatorValues();
    }
}
