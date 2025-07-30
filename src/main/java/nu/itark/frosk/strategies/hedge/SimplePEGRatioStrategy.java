package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SimplePEGRatioStrategy extends AbstractStrategy implements IIndicatorValue  {

    private final BarSeriesService barSeriesService;

    /**
     * PEG Ratio Strategy: Buy if PEG ratio < 1.5
     * Note: PEG ratio is retrieved externally and applied as a static filter
     */
    public Strategy buildStrategy(BarSeries series) {
        double pegRatio =  getPEGRatio(series.getName());
        Num pegThreshold = series.numOf(1.5);

        Rule entryRule;
        Rule exitRule;

        if (pegRatio > 0 && pegRatio < pegThreshold.doubleValue()) {
            // PEG is favorable → enable strategy
            entryRule = BooleanRule.TRUE;  // Always enter (subject to additional filters if needed)
            exitRule = BooleanRule.FALSE;  // Never exit based on PEG alone
        } else {
            // PEG too high → no trade
            entryRule = BooleanRule.FALSE;
            exitRule = BooleanRule.TRUE;   // Always exit if in position
        }

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
