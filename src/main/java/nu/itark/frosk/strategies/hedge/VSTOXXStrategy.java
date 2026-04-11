package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.VSTOXXRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.VSTOXXRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * VSTOXX above 25 AND rising → risk-off signal.
 * European volatility index — a more direct risk measure for OMXS30 than VIX alone.
 */
@Component
@RequiredArgsConstructor
public class VSTOXXStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "^V2TX";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule = new BooleanIndicatorRule(new VSTOXXRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new VSTOXXRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
