package nu.itark.frosk.strategies;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator.ConvergenceDivergenceType;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;

public class ConvergenceDivergenceStrategy {

	
	TimeSeries series = null;
	   
	public ConvergenceDivergenceStrategy(TimeSeries series) {
		this.series = series;
	}		

    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        int timePeriod = 12;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        VolumeIndicator volume = new VolumeIndicator(series);
        ConvergenceDivergenceIndicator.ConvergenceDivergenceType posDivType = ConvergenceDivergenceType.positiveDivergent;
        ConvergenceDivergenceIndicator.ConvergenceDivergenceType negDivType = ConvergenceDivergenceType.negativeDivergent;       		
        ConvergenceDivergenceIndicator posDiv = new ConvergenceDivergenceIndicator(closePrice,volume,timePeriod,posDivType,0.1,0.1);
        ConvergenceDivergenceIndicator negDiv = new ConvergenceDivergenceIndicator(closePrice,volume,timePeriod,negDivType,0.1,0.1);
                		      		 
        Rule entryRule = new BooleanIndicatorRule(posDiv);
        Rule exitRule = new BooleanIndicatorRule(negDiv);    
        
        
        Strategy strategy = new BaseStrategy("ConvergenceDivergenceStrategy", entryRule, exitRule);
        return strategy;
    }	
	
	
	
}
