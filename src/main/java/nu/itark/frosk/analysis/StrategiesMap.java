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

		ADXStrategy adxStrat = new ADXStrategy(series);
		strategies.put(adxStrat.buildStrategy(), ADXStrategy.class.getSimpleName());

		RunawayGAPStrategy baGapStrat = new RunawayGAPStrategy(series);
		strategies.put(baGapStrat.buildStrategy(), RunawayGAPStrategy.class.getSimpleName());

		CCICorrectionStrategy cciStrat = new CCICorrectionStrategy(series);
		strategies.put(cciStrat.buildStrategy(), CCICorrectionStrategy.class.getSimpleName());

		ConvergenceDivergenceStrategy cdStrat = new ConvergenceDivergenceStrategy(series);
		strategies.put(cdStrat.buildStrategy(), ConvergenceDivergenceStrategy.class.getSimpleName());

		EngulfingStrategy engStrat = new EngulfingStrategy(series);
		strategies.put(engStrat.buildStrategy(), EngulfingStrategy.class.getSimpleName());

		GlobalExtremaStrategy geStrat = new GlobalExtremaStrategy(series);
		strategies.put(geStrat.buildStrategy(), GlobalExtremaStrategy.class.getSimpleName());

		HaramiStrategy haramisStrat = new HaramiStrategy(series);
		strategies.put(haramisStrat.buildStrategy(), HaramiStrategy.class.getSimpleName());

		MovingMomentumStrategy mmStrat = new MovingMomentumStrategy(series);
		strategies.put(mmStrat.buildStrategy(), MovingMomentumStrategy.class.getSimpleName());

		RSI2Strategy rsiStrat = new RSI2Strategy(series);
		strategies.put(rsiStrat.buildStrategy(), RSI2Strategy.class.getSimpleName());

		SimpleMovingMomentumStrategy simpleManStrat = new SimpleMovingMomentumStrategy(series);
		strategies.put(simpleManStrat.buildStrategy(), SimpleMovingMomentumStrategy.class.getSimpleName());

		ThreeBlackWhiteStrategy threeStrat = new ThreeBlackWhiteStrategy(series);
		strategies.put(threeStrat.buildStrategy(), ThreeBlackWhiteStrategy.class.getSimpleName());

		VWAPStrategy wvapStrat = new VWAPStrategy(series);
		strategies.put(wvapStrat.buildStrategy(), VWAPStrategy.class.getSimpleName());

		EMATenTwentyStrategy ema1020Strat = new EMATenTwentyStrategy(series);
		strategies.put(ema1020Strat.buildStrategy(), EMATenTwentyStrategy.class.getSimpleName());

		return strategies;
	}
	
	public static List<String> buildStrategiesMap() {
		List<String> strategies = new ArrayList<String>();
		strategies.add(ADXStrategy.class.getSimpleName());
		strategies.add(RunawayGAPStrategy.class.getSimpleName());
		strategies.add(CCICorrectionStrategy.class.getSimpleName());
		strategies.add(ConvergenceDivergenceStrategy.class.getSimpleName());
		strategies.add(EngulfingStrategy.class.getSimpleName());
		strategies.add(GlobalExtremaStrategy.class.getSimpleName());
		strategies.add(HaramiStrategy.class.getSimpleName());
		strategies.add(MovingMomentumStrategy.class.getSimpleName());
		strategies.add(RSI2Strategy.class.getSimpleName());
		strategies.add(SimpleMovingMomentumStrategy.class.getSimpleName());
		strategies.add(ThreeBlackWhiteStrategy.class.getSimpleName());
		strategies.add(VWAPStrategy.class.getSimpleName());
		strategies.add(EMATenTwentyStrategy.class.getSimpleName());
		return strategies;
	}

	public static List<Strategy> getStrategies(BarSeries series) {
		List<Strategy> strategies = new ArrayList<Strategy>();
		strategies.add(new ADXStrategy(series).buildStrategy());
		strategies.add(new RunawayGAPStrategy(series).buildStrategy());
		strategies.add(new CCICorrectionStrategy(series).buildStrategy());
		strategies.add(new ConvergenceDivergenceStrategy(series).buildStrategy());
		strategies.add(new EngulfingStrategy(series).buildStrategy());
		strategies.add(new GlobalExtremaStrategy(series).buildStrategy());
		strategies.add(new HaramiStrategy(series).buildStrategy());
		strategies.add(new MovingMomentumStrategy(series).buildStrategy());
		strategies.add(new RSI2Strategy(series).buildStrategy());
		strategies.add(new SimpleMovingMomentumStrategy(series).buildStrategy());
		strategies.add(new ThreeBlackWhiteStrategy(series).buildStrategy());
		strategies.add(new VWAPStrategy(series).buildStrategy());
		return strategies;
	}



}
