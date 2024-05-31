package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator.ConvergenceDivergenceType;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Component
public class ConvergenceDivergenceStrategy extends AbstractStrategy implements IIndicatorValue {

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;
        int timePeriod = 12;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        VolumeIndicator volume = new VolumeIndicator(series);
        ConvergenceDivergenceIndicator.ConvergenceDivergenceType posDivType = ConvergenceDivergenceType.positiveDivergent;
        ConvergenceDivergenceIndicator.ConvergenceDivergenceType negDivType = ConvergenceDivergenceType.negativeDivergent;       		
        ConvergenceDivergenceIndicator posDiv = new ConvergenceDivergenceIndicator(closePrice,volume,timePeriod,posDivType,0.1,0.1);
        ConvergenceDivergenceIndicator negDiv = new ConvergenceDivergenceIndicator(closePrice,volume,timePeriod,negDivType,0.1,0.1);
        Rule entryRule = new BooleanIndicatorRule(posDiv);
        Rule exitRule;
        if (!inherentExitRule) {
            exitRule = new BooleanIndicatorRule(negDiv);
        } else {
            exitRule = exitRule();
        }

        Strategy strategy = new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
        return strategy;
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
	
}
