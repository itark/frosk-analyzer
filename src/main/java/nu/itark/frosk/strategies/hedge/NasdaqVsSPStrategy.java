package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.ReturnOverPeriodIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NasdaqVsSPStrategy implements IIndicatorValue {
    private final BarSeriesService barSeriesService;

    /**
     * Rule: NASDAQ underperforms S&P over 30 days.
     * Entry signal when NASDAQ return over 30 bars < S&P return over 30 bars.
     *
     *
     */
    public Strategy buildStrategy() {
        BarSeries nasdaqSeries = barSeriesService.getDataSet("^IXIC", false, false);
        BarSeries sp500Series = barSeriesService.getDataSet("^GSPC", false, false);

        // Align both series by timestamp and truncate to match
        List<BarSeries> aligned = BarSeriesAligner.alignAndTruncate(List.of(nasdaqSeries, sp500Series),  nasdaqSeries.getBarCount());
        BarSeries alignedNasdaq = aligned.get(0);
        BarSeries alignedSP500 = aligned.get(1);

        ClosePriceIndicator nasdaqClose = new ClosePriceIndicator(alignedNasdaq);
        ClosePriceIndicator sp500Close = new ClosePriceIndicator(alignedSP500);

        // Create return over 30 bars
        ReturnOverPeriodIndicator nasdaqReturn = new ReturnOverPeriodIndicator(nasdaqClose, 30);
        ReturnOverPeriodIndicator sp500Return = new ReturnOverPeriodIndicator(sp500Close, 30);

        // Entry rule: NASDAQ return is less than S&P return (underperformance)
        Rule entryRule = new UnderIndicatorRule(nasdaqReturn, sp500Return);

        // Exit rule: the opposite (you can tune this logic further)
        Rule exitRule = entryRule.negation();

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}

