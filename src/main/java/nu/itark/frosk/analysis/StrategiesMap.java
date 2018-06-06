package nu.itark.frosk.analysis;

import java.util.HashMap;
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
//		strategies.put(CCICorrectionStrategy.buildStrategy(series), CCICorrectionStrategy.class.getSimpleName());
//		strategies.put(GlobalExtremaStrategy.buildStrategy(series), GlobalExtremaStrategy.class.getSimpleName());
//		strategies.put(MovingMomentumStrategy.buildStrategy(series), MovingMomentumStrategy.class.getSimpleName());
		strategies.put(RSI2Strategy.buildStrategy(series), RSI2Strategy.class.getSimpleName());
		return strategies;
	}
	
}
