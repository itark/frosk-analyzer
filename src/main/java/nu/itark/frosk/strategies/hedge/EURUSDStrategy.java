package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.EURUSDRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.EURUSDRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * EUR/USD drops more than 3% in 10 days → risk-off signal.
 */
@Component
@RequiredArgsConstructor
public class EURUSDStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "EURUSD=X";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule = new BooleanIndicatorRule(new EURUSDRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new EURUSDRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
