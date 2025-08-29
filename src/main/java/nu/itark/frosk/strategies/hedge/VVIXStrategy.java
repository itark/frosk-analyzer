package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * VVIXStrategy
 *
 * Entry:  VVIX > vvixThreshold AND ROC(vvixClose, rocPeriod) > 0  (high and rising)
 * Exit:   VVIX <= vvixThreshold OR ROC <= 0
 */
@Component
public class VVIXStrategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;

    // configurable threshold / periods (tune as needed)
    private final double vvixThreshold = 110.0;
    private final int rocPeriod = 5;

    public VVIXStrategy(BarSeriesService barSeriesService) {
        this.barSeriesService = barSeriesService;
    }

    public Strategy buildStrategy() {
        // Use the VVIX ticker your data source provides. Adjust if different.
        BarSeries vvixSeries = barSeriesService.getDataSet("^VVIX", false, false);

        ClosePriceIndicator close = new ClosePriceIndicator(vvixSeries);

        // ROC(5) positive => rising
        ROCIndicator roc = new ROCIndicator(close, rocPeriod);

        Num threshold = vvixSeries.numOf(vvixThreshold);
        Num zero = vvixSeries.numOf(0);

        // Entry: VVIX > threshold AND ROC > 0
        Rule vvixHigh = new OverIndicatorRule(close, threshold);
        Rule rocPositive = new OverIndicatorRule(roc, zero);
        Rule entryRule = new AndRule(vvixHigh, rocPositive);

        // Exit: NOT(entry) -> either VVIX <= threshold OR ROC <= 0
        Rule exitRule = entryRule.negation();

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
