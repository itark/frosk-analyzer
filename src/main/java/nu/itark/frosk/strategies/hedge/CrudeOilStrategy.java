package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.CrudeOilRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.CrudeOilRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CrudeOilStrategy implements IIndicatorValue {
    final BarSeriesService barSeriesService;
    final String securityName = "CL=F";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        CrudeOilRiskOnIndicator crudeOilRiskOnIndicator = new CrudeOilRiskOnIndicator(barSeries);
        Rule onRule = new BooleanIndicatorRule(crudeOilRiskOnIndicator);
        CrudeOilRiskOffIndicator crudeOilRiskOffIndicator = new CrudeOilRiskOffIndicator(barSeries);
        Rule offRule =  new BooleanIndicatorRule(crudeOilRiskOffIndicator);
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

}
