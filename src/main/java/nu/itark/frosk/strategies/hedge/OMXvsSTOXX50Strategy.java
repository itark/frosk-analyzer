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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Risk-off when STOXX50 30-day return outperforms OMX30 by more than 3 percentage points.
 * Domestic underperformance relative to Europe signals Sweden-specific risk-off leadership.
 */
@Component
@RequiredArgsConstructor
public class OMXvsSTOXX50Strategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;

    /**
     * @param series the ^STOXX50E BarSeries passed in by the executor
     */
    public Strategy buildStrategy(BarSeries series) {
        BarSeries omxSeries = barSeriesService.getDataSet("^OMX", false, false);

        // Build a timestamp-to-close map for OMX so we can look up by date
        Map<ZonedDateTime, Num> omxCloseByDate = new HashMap<>();
        for (int i = 0; i < omxSeries.getBarCount(); i++) {
            Bar bar = omxSeries.getBar(i);
            omxCloseByDate.put(bar.getEndTime(), bar.getClosePrice());
        }

        ReturnOverPeriodIndicator stoxx50Return = new ReturnOverPeriodIndicator(
                new ClosePriceIndicator(series), 30);

        // Spread indicator that works on the executor's series, looking up OMX by date
        CachedIndicator<Num> spread = new CachedIndicator<>(series) {
            @Override
            protected Num calculate(int index) {
                Num stoxx50Ret = stoxx50Return.getValue(index);

                // Compute OMX 30-day return by date lookup
                ZonedDateTime currentDate = series.getBar(index).getEndTime();
                int lookback = Math.min(30, index);
                ZonedDateTime pastDate = series.getBar(index - lookback).getEndTime();

                Num omxCurrent = omxCloseByDate.get(currentDate);
                Num omxPast = omxCloseByDate.get(pastDate);

                if (omxCurrent == null || omxPast == null || omxPast.isZero()) {
                    return series.numOf(0);
                }

                Num omxRet = omxCurrent.minus(omxPast).dividedBy(omxPast);
                return stoxx50Ret.minus(omxRet);
            }
            @Override
            public int getUnstableBars() { return 30; }
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
