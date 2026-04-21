package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.ReturnOverPeriodIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * Risk-off when STOXX50 30-day return outperforms OMX30 by more than 3 percentage points.
 * Domestic underperformance relative to Europe signals Sweden-specific risk-off leadership.
 */
@Component
@RequiredArgsConstructor
public class OMXvsSTOXX50Strategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;

    public Strategy buildStrategy() {
        BarSeries stoxx50Series = barSeriesService.getDataSet("^STOXX50E", false, false);
        BarSeries omxSeries = barSeriesService.getDataSet("^OMX", false, false);

        List<BarSeries> aligned = BarSeriesAligner.alignAndTruncate(
                List.of(stoxx50Series, omxSeries), stoxx50Series.getBarCount());
        BarSeries alignedSTOXX50 = aligned.get(0);
        BarSeries alignedOMX = aligned.get(1);

        ReturnOverPeriodIndicator stoxx50Return = new ReturnOverPeriodIndicator(
                new ClosePriceIndicator(alignedSTOXX50), 30);
        ReturnOverPeriodIndicator omxReturn = new ReturnOverPeriodIndicator(
                new ClosePriceIndicator(alignedOMX), 30);

        // spread = stoxx50Return - omxReturn
        CachedIndicator<Num> spread = new CachedIndicator<>(alignedSTOXX50) {
            @Override
            protected Num calculate(int index) {
                return stoxx50Return.getValue(index).minus(omxReturn.getValue(index));
            }
            @Override
            public int getUnstableBars() { return 0; }
        };

        // Risk-off (SELL): STOXX50 outperforms OMX by > 3pp (0.03 in decimal)
        Rule exitRule = new OverIndicatorRule(spread, 0.03);
        // Risk-on (BUY): spread not exceeding 3pp
        Rule entryRule = exitRule.negation();

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
