package nu.itark.frosk.strategies.hedge;

import lombok.extern.slf4j.Slf4j;
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
/**
 Beta on Yahoo Finance measures a stock's volatility relative to the overall market (usually the S&P 500).

 Beta = 1.0: The stock moves in line with the market.
 Beta > 1.0: The stock is more volatile than the market (e.g., β = 1.5 means 50% more volatile).
 Beta < 1.0: The stock is less volatile (e.g., β = 0.8 means 20% less volatile).
 Beta < 0: The stock tends to move in the opposite direction of the market (rare, e.g., some inverse ETFs).
 🔹 Calculation:
 Based on 5-year monthly returns compared to the S&P 500. It uses regression analysis to determine sensitivity.

 🔹 Use:

 High beta: Higher risk/return potential (aggressive investing).
 Low beta: More stable, defensive (suitable for conservative portfolios).

 A stock with β = 1.3 is expected to rise 13% when the market rises 10%, but also fall 13% if the market drops 10%.
 */


@Slf4j
public class BetaStrategy extends AbstractStrategy implements IIndicatorValue {
    /**
     * Beta (5Y Monthly): > 1.3
     */
    @Value("${frosk.hedge.criteria.betathreshold}")
    private Double betaThreshold;

    public Strategy buildStrategy(BarSeries series) {
        Double beta = getBeta(series.getName());
        log.info("beta: {} for {}", beta, series.getName());
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 10);
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);
        // Entry rule: short SMA crosses above long SMA AND beta is high enough
        Rule betaRule = (beta > betaThreshold) ? BooleanRule.TRUE : BooleanRule.FALSE;
        Rule entryRule = new OverIndicatorRule(shortSma, longSma) // crossover
                .and(betaRule); // Only enter if beta is high
        // Exit rule: short SMA crosses below long SMA
        Rule exitRule = new UnderIndicatorRule(shortSma, longSma);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
