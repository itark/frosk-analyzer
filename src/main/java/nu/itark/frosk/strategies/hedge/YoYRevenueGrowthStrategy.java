package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Component
public class YoYRevenueGrowthStrategy extends AbstractStrategy implements IIndicatorValue {

    /**
     * YoY growth > 15%
     */
    @Value("${frosk.hedge.criteria.yoygrowth.threshold}")
    private Double yoyGrowthThreshold;

    public Strategy buildStrategy(BarSeries series) {
        Double yoYGrowth = getYoYGrowth(series.getName());
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 10);
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);
        // Entry rule: short SMA crosses above long SMA AND yoYGrowth is high enough
        Rule betaRule = (yoYGrowth > yoyGrowthThreshold) ? BooleanRule.TRUE : BooleanRule.FALSE;
        Rule entryRule = new OverIndicatorRule(shortSma, longSma) // crossover
                .and(betaRule); // Only enter if yoYGrowth is high
        // Exit rule: short SMA crosses below long SMA
        Rule exitRule = new UnderIndicatorRule(shortSma, longSma);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
