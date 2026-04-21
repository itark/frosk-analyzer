package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.TreasuryYield10YRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.TreasuryYield10YRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * Risk-off when US 10-Year Treasury Yield (^TNX) is above 4.5% AND rising.
 * High and rising long rates lift global discount rates, compressing equity valuations.
 */
@Component
@RequiredArgsConstructor
public class TreasuryYieldStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "^TNX";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule  = new BooleanIndicatorRule(new TreasuryYield10YRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new TreasuryYield10YRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
