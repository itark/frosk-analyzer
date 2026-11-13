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
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private final BarSeriesService barSeriesService;

    // configurable threshold / periods (tune as needed)
    // Risk-off when VVIX is above this and rising (see buildStrategy). 125 targets genuine
    // vol-of-vol spikes rather than the elevated-but-normal 100-120 range. Tune via backtest.
    private final double vvixThreshold = 125.0;
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

        // "VVIX > threshold AND rising" is the risk-OFF (elevated vol-of-vol) condition.
        // Convention (see VIXStrategy/SP500Strategy): BaseStrategy(name, lowRiskRule, riskRule)
        // where entry(BUY)=risk-on and exit(SELL)=risk-off (HedgeIndexService maps SELL->risk=TRUE).
        // Previously this condition was placed in the ENTRY (BUY) slot, which recorded high &
        // rising VVIX as risk-ON — inverted. It made VVIX risk-off ~90% of the time (whenever
        // vol-of-vol was NOT spiking) and left it stuck risk-off after the last low-VVIX exit.
        Rule vvixHigh = new OverIndicatorRule(close, threshold);
        Rule rocPositive = new OverIndicatorRule(roc, zero);
        Rule riskRule = new AndRule(vvixHigh, rocPositive);   // elevated & rising -> risk-off (SELL)
        Rule lowRiskRule = riskRule.negation();               // otherwise -> risk-on (BUY)

        return new BaseStrategy(this.getClass().getSimpleName(), lowRiskRule, riskRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
