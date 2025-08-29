package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.CrossedDownIndicatorRule;
import nu.itark.frosk.strategies.indicators.CrossedUpIndicatorRule;
import nu.itark.frosk.strategies.indicators.HighestValueIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoldStrategy implements IIndicatorValue {
    final BarSeriesService barSeriesService;
    final HedgeIndexService hedgeIndexService;
    int lookbackPeriod = 10;

    /**
     * Breaks above 10-day high
     * @return
     */
    public Strategy buildStrategy() {
        BarSeries barSeries = barSeriesService.getDataSet("GC=F", false, false);
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
        HighestValueIndicator tenDayHigh = new HighestValueIndicator(closePriceIndicator, lookbackPeriod);
        Indicator<Num> laggedTenDayHigh = new PreviousValueIndicator(tenDayHigh);
        //BUY, low risk
        Rule lowRiskRule = new CrossedUpIndicatorRule(closePriceIndicator, laggedTenDayHigh);
        //SELL, risk
        Rule riskRule = new CrossedDownIndicatorRule(closePriceIndicator, laggedTenDayHigh);

        return new BaseStrategy(this.getClass().getSimpleName(), lowRiskRule, riskRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
