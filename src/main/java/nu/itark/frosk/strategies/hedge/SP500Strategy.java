package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Component
public class SP500Strategy implements IIndicatorValue {
    final BarSeriesService barSeriesService;
    final HedgeIndexService hedgeIndexService;

    public SP500Strategy(BarSeriesService barSeriesService, HedgeIndexService hedgeIndexService) {
        this.barSeriesService = barSeriesService;
        this.hedgeIndexService = hedgeIndexService;
    }

    /**
     * S&P 500 Below 200-day MA
     * @return
     */
    public Strategy buildStrategy() {
        BarSeries barSeries = barSeriesService.getDataSet("^GSPC", false, false);
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
        SMAIndicator longSma = new SMAIndicator(closePriceIndicator, 200);
        Rule onRule = new CrossedDownIndicatorRule(closePriceIndicator,longSma );
        Rule offRule = new CrossedUpIndicatorRule(closePriceIndicator,longSma );
        return new BaseStrategy(this.getClass().getSimpleName(), onRule, offRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
