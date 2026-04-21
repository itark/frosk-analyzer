package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.DXYRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.DXYRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * DXY above 105 AND rising → risk-off signal.
 * A strong, rising dollar tightens global financial conditions.
 */
@Component
@RequiredArgsConstructor
public class DXYStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "DX-Y.NYB";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule = new BooleanIndicatorRule(new DXYRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new DXYRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
