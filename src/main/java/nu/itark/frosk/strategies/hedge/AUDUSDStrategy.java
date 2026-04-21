package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.AUDUSDRiskOffIndicator;
import nu.itark.frosk.strategies.indicators.AUDUSDRiskOnIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
 * AUD/USD drops more than 2% in 5 days → risk-off signal (commodity/risk currency weakness).
 */
@Component
@RequiredArgsConstructor
public class AUDUSDStrategy implements IIndicatorValue {

    final BarSeriesService barSeriesService;
    final String securityName = "AUDUSD=X";

    public Strategy buildStrategy() {
        final BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        Rule onRule = new BooleanIndicatorRule(new AUDUSDRiskOnIndicator(barSeries));
        Rule offRule = new BooleanIndicatorRule(new AUDUSDRiskOffIndicator(barSeries));
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
