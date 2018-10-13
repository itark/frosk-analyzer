package nu.itark.frosk.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.strategies.CCICorrectionStrategy;
import nu.itark.frosk.strategies.GlobalExtremaStrategy;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;

public class StrategiesMap {

	public static Map<Strategy, String> buildStrategiesMap(TimeSeries series) {
		HashMap<Strategy, String> strategies = new HashMap<>();
		RSI2Strategy rsiStrat = new RSI2Strategy(series);
		strategies.put(rsiStrat.buildStrategy(), RSI2Strategy.class.getSimpleName());
	
		MovingMomentumStrategy mmStrat = new MovingMomentumStrategy(series);
		strategies.put(mmStrat.buildStrategy(), MovingMomentumStrategy.class.getSimpleName());		
	
		GlobalExtremaStrategy geStrat = new GlobalExtremaStrategy(series);
		strategies.put(geStrat.buildStrategy(), GlobalExtremaStrategy.class.getSimpleName());			
	
		CCICorrectionStrategy cciStrat = new CCICorrectionStrategy(series);
		strategies.put(cciStrat.buildStrategy(), CCICorrectionStrategy.class.getSimpleName());			
		

		return strategies;
	}
	
	public static List<String> buildStrategiesMap() {
		List<String> strategies = new ArrayList<String>();
		strategies.add(RSI2Strategy.class.getSimpleName());
		strategies.add(MovingMomentumStrategy.class.getSimpleName());
		strategies.add(GlobalExtremaStrategy.class.getSimpleName());
		strategies.add(CCICorrectionStrategy.class.getSimpleName());
	
		return strategies;
	}	
	
	
	
}
