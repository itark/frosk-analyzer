package nu.itark.frosk.strategies.hedge;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.BetaIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Component
@Slf4j
public class BetaStrategy implements IIndicatorValue {
    @Value("${frosk.criteria.beta}")
    private Number beta;
    private BarSeriesService barSeriesService;
    private HedgeIndexService hedgeIndexService;

    public BetaStrategy(BarSeriesService barSeriesService, HedgeIndexService hedgeIndexService) {
        this.barSeriesService = barSeriesService;
        this.hedgeIndexService = hedgeIndexService;
    }

    public Strategy buildStrategy(BarSeries stockSeries) {
        BarSeries marketSeries = barSeriesService.getDataSet("^GSPC", false, false);
        ClosePriceIndicator stockClose = new ClosePriceIndicator(stockSeries);
        ClosePriceIndicator marketClose = new ClosePriceIndicator(marketSeries);
        // 60 = approx. 3 months of daily returns
        BetaIndicator betaIndicator = new BetaIndicator(stockClose, marketClose, 60);
        // Define a threshold for high beta (e.g., > 1.3)
        Num betaThreshold = stockSeries.numOf(beta);
        // Define entry rule: enter if beta is high (risk-on condition)
        Rule entryRule = new OverIndicatorRule(betaIndicator, betaThreshold);
        // Define exit rule: exit if beta drops below threshold
        Rule exitRule = new UnderIndicatorRule(betaIndicator, betaThreshold);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
