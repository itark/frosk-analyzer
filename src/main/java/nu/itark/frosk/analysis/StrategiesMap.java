package nu.itark.frosk.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.itark.frosk.strategies.*;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;

public class StrategiesMap {

	public static Map<Strategy, String> buildStrategiesMap(BarSeries series) {
		HashMap<Strategy, String> strategies = new HashMap<>();
		RSI2Strategy rsiStrat = new RSI2Strategy(series);
		strategies.put(rsiStrat.buildStrategy(), RSI2Strategy.class.getSimpleName());
	
		MovingMomentumStrategy mmStrat = new MovingMomentumStrategy(series);
		strategies.put(mmStrat.buildStrategy(), MovingMomentumStrategy.class.getSimpleName());		
	
		GlobalExtremaStrategy geStrat = new GlobalExtremaStrategy(series);
		strategies.put(geStrat.buildStrategy(), GlobalExtremaStrategy.class.getSimpleName());			
	
		CCICorrectionStrategy cciStrat = new CCICorrectionStrategy(series);
		strategies.put(cciStrat.buildStrategy(), CCICorrectionStrategy.class.getSimpleName());			
		

		EngulfingStrategy engStrat = new EngulfingStrategy(series);
		strategies.put(engStrat.buildStrategy(), EngulfingStrategy.class.getSimpleName());				
		
		HaramiStrategy haramisStrat = new HaramiStrategy(series);
		strategies.put(haramisStrat.buildStrategy(), HaramiStrategy.class.getSimpleName());				
		
		ThreeBlackWhiteStrategy threeStrat = new ThreeBlackWhiteStrategy(series);
		strategies.put(threeStrat.buildStrategy(), ThreeBlackWhiteStrategy.class.getSimpleName());

		SimpleMovingMomentumStrategy simpleManStrat = new SimpleMovingMomentumStrategy(series);
		strategies.put(simpleManStrat.buildStrategy(), SimpleMovingMomentumStrategy.class.getSimpleName());

		VWAPStrategy wvapStrat = new VWAPStrategy(series);
		strategies.put(wvapStrat.buildStrategy(), VWAPStrategy.class.getSimpleName());

		return strategies;
	}
	
	public static List<String> buildStrategiesMap() {
		List<String> strategies = new ArrayList<String>();
		strategies.add(RSI2Strategy.class.getSimpleName());
		strategies.add(MovingMomentumStrategy.class.getSimpleName());
		strategies.add(SimpleMovingMomentumStrategy.class.getSimpleName());
		strategies.add(GlobalExtremaStrategy.class.getSimpleName());
		strategies.add(CCICorrectionStrategy.class.getSimpleName());
		strategies.add(EngulfingStrategy.class.getSimpleName());
		strategies.add(HaramiStrategy.class.getSimpleName());
		strategies.add(ThreeBlackWhiteStrategy.class.getSimpleName());
		return strategies;
	}

	public static List<Strategy> getStrategies(BarSeries series) {
		List<Strategy> strategies = new ArrayList<Strategy>();
		strategies.add(new RSI2Strategy(series).buildStrategy());
		strategies.add(new MovingMomentumStrategy(series).buildStrategy());
		strategies.add(new SimpleMovingMomentumStrategy(series).buildStrategy());
		strategies.add(new GlobalExtremaStrategy(series).buildStrategy());
		strategies.add(new CCICorrectionStrategy(series).buildStrategy());
		strategies.add(new EngulfingStrategy(series).buildStrategy());
		strategies.add(new HaramiStrategy(series).buildStrategy());
		strategies.add(new ThreeBlackWhiteStrategy(series).buildStrategy());
		strategies.add(new ADXStrategy().buildStrategy(series));
		return strategies;
	}



}
