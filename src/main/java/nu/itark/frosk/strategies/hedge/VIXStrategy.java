package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.VixRiskIndicator;
import nu.itark.frosk.strategies.indicators.VixLowRiskIndicator;
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
        VixLowRiskIndicator vixLowRiskIndicator = new VixLowRiskIndicator(barSeries, hedgeIndexService);
        //BUY, low risk
        Rule lowRiskRule = new BooleanIndicatorRule(vixLowRiskIndicator);
        VixRiskIndicator vixOffIndicator = new VixRiskIndicator(barSeries);
        //SELL, risk
        Rule riskRule = new BooleanIndicatorRule(vixOffIndicator);
        return new BaseStrategy(this.getClass().getSimpleName(), lowRiskRule, riskRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
