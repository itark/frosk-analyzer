package nu.itark.frosk.analysis;

import java.util.HashMap;
import java.util.Map;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.strategies.GlobalExtremaStrategy;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;

public class StrategiesMap {

	public static Map<Strategy, String> buildStrategiesMap(TimeSeries series) {
		HashMap<Strategy, String> strategies = new HashMap<>();
		strategies.put(GlobalExtremaStrategy.buildStrategy(series), GlobalExtremaStrategy.class.getSimpleName());
		MovingMomentumStrategy mmStrat = new MovingMomentumStrategy(series);
		strategies.put(mmStrat.buildStrategy(), MovingMomentumStrategy.class.getSimpleName());
		RSI2Strategy rsiStrat = new RSI2Strategy(series);
		strategies.put(rsiStrat.buildStrategy(), RSI2Strategy.class.getSimpleName());
		return strategies;
	}
	
}
