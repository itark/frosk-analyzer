package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.USDJPYRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.USDJPYRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * USD/JPY drops more than 2% in 5 days (JPY strengthening) → risk-off signal.
 */
@Component
@RequiredArgsConstructor
public class USDJPYStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "JPY=X";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule = new BooleanIndicatorRule(new USDJPYRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new USDJPYRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
