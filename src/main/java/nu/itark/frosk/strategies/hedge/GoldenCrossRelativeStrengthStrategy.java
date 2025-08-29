package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OrRule;

/**
 * Golden Cross Relative Strength Strategy
 *
 * Entry Conditions:
 * - Price is above both 50D and 200D SMA (Strong Relative Strength)
 * - Optional: 50D SMA is above 200D SMA (Golden Cross pattern)
 *
 * Exit Conditions:
 * - Price falls below 50D SMA (loss of relative strength)
 * - OR 50D SMA falls below 200D SMA (Death Cross)
 */
@Component
public class GoldenCrossRelativeStrengthStrategy {

    public Strategy buildStrategy(BarSeries series) {
       ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
       SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
       SMAIndicator sma200 = new SMAIndicator(closePrice, 200);

        // Entry Rules
        Rule priceAbove50D = new OverIndicatorRule(closePrice, sma50);
        Rule priceAbove200D = new OverIndicatorRule(closePrice, sma200);
        Rule goldenCross = new OverIndicatorRule(sma50, sma200);

        // Strong entry: Price above both MAs AND Golden Cross pattern
        Rule strongEntry = new AndRule(
                new AndRule(priceAbove50D, priceAbove200D),
                goldenCross
        );

        // Moderate entry: Price above both MAs (even without Golden Cross)
        Rule moderateEntry = new AndRule(priceAbove50D, priceAbove200D);

        // Combined entry rule (prefer strong entry, but allow moderate)
        Rule entryRule = new OrRule(strongEntry, moderateEntry);

        // Exit Rules
        Rule priceBelowMediumMA = new UnderIndicatorRule(closePrice, sma50);
        Rule deathCross = new UnderIndicatorRule(sma50, sma200);

        // Exit when losing relative strength OR death cross occurs
        Rule exitRule = new OrRule(priceBelowMediumMA, deathCross);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /**
     * Alternative strategy that REQUIRES Golden Cross for entry
     */
    public Strategy buildStrictGoldenCrossStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        // Entry Rules - ALL conditions must be met
        Rule priceAbove50D = new OverIndicatorRule(closePrice, sma50);
        Rule priceAbove200D = new OverIndicatorRule(closePrice, sma200);
        Rule goldenCross = new OverIndicatorRule(sma50, sma200);

        // Strict entry: ALL conditions required
        Rule strictEntryRule = new AndRule(
                new AndRule(priceAbove50D, priceAbove200D),
                goldenCross
        );

        // Exit Rules
        Rule priceBelowMediumMA = new UnderIndicatorRule(closePrice, sma50);
        Rule deathCross = new UnderIndicatorRule(sma50, sma200);

        Rule exitRule = new OrRule(priceBelowMediumMA, deathCross);

        return new BaseStrategy(this.getClass().getSimpleName(), strictEntryRule, exitRule);
    }

}
