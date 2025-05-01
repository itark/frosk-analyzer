package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.VixRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.VixRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

@Component
public class VIXStrategy implements IIndicatorValue {
    final BarSeriesService barSeriesService;
    final HedgeIndexService hedgeIndexService;

    public VIXStrategy(BarSeriesService barSeriesService, HedgeIndexService hedgeIndexService) {
        this.barSeriesService = barSeriesService;
        this.hedgeIndexService = hedgeIndexService;
    }

    public Strategy buildStrategy() {
        BarSeries barSeries = barSeriesService.getDataSet("^VIX", false, false);
        VixRiskOnIndicator vixOnIndicator = new VixRiskOnIndicator(barSeries, hedgeIndexService);
        Rule onRule = new BooleanIndicatorRule(vixOnIndicator);
        VixRiskOffIndicator vixOffIndicator = new VixRiskOffIndicator(barSeries);
        Rule offRule = new BooleanIndicatorRule(vixOffIndicator);
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
