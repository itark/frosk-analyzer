package nu.itark.frosk.analysis;

import lombok.Data;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.*;
import nu.itark.frosk.strategies.hedge.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Data
public class StrategiesMap {

	@Value("${frosk.strategies.exclude}")
	private String[] excludesStrategies;

	@Autowired
	private EngulfingStrategy engulfingStrategy;
	@Autowired
	private RSI2Strategy rsiStrategy;
	@Autowired
	private MovingMomentumStrategy movingMomentumStrategy;
	@Autowired
	private SimpleMovingMomentumStrategy simpleMovingMomentumStrategy;
	@Autowired
	private GlobalExtremaStrategy globalExtremaStrategy;
	@Autowired
	private CCICorrectionStrategy cciCorrectionStrategy;
	@Autowired
	private HaramiStrategy haramiStrategy;
	@Autowired
	private ThreeBlackWhiteStrategy threeBlackWhiteStrategy;
	@Autowired
	private ADXStrategy adxStrategy;
	@Autowired
	private ConvergenceDivergenceStrategy convergenceDivergenceStrategy;
	@Autowired
	private VWAPStrategy vwapStrategy;
	@Autowired
	private RunawayGAPStrategy runawayGAPStrategy;
	@Autowired
	private EMATenTwentyStrategy emaTenTwentyStrategy;
	@Autowired
	private EMATenTenStrategy emaTenTenStrategy;
	@Autowired
	private HedgeIndexStrategy hedgeIndexStrategy;
	@Autowired
	private VIXStrategy vixStrategy;
	@Autowired
	private CrudeOilStrategy crudeOilStrategy;
	@Autowired
	private GoldStrategy goldStrategy;
	@Autowired
	private SP500Strategy sp500Strategy;
	@Autowired
	private BetaStrategy betaStrategy;
	@Autowired
	private HighLanderStrategy highLanderStrategy;

	private List<Strategy> strategies = null;

	public List<String> buildStrategiesMap() {
		List<String> strategies = new ArrayList<String>();
		strategies.add(adxStrategy.getClass().getSimpleName());
		strategies.add(runawayGAPStrategy.getClass().getSimpleName());
		strategies.add(cciCorrectionStrategy.getClass().getSimpleName());
		strategies.add(convergenceDivergenceStrategy.getClass().getSimpleName());
		strategies.add(engulfingStrategy.getClass().getSimpleName());
		strategies.add(globalExtremaStrategy.getClass().getSimpleName());
		strategies.add(haramiStrategy.getClass().getSimpleName());
		strategies.add(movingMomentumStrategy.getClass().getSimpleName());
		strategies.add(rsiStrategy.getClass().getSimpleName());
		strategies.add(simpleMovingMomentumStrategy.getClass().getSimpleName());
		strategies.add(threeBlackWhiteStrategy.getClass().getSimpleName());
		strategies.add(vwapStrategy.getClass().getSimpleName());
		strategies.add(emaTenTwentyStrategy.getClass().getSimpleName());
		strategies.add(emaTenTenStrategy.getClass().getSimpleName());
		strategies.add(hedgeIndexStrategy.getClass().getSimpleName());
		strategies.add(vixStrategy.getClass().getSimpleName());
		strategies.add(crudeOilStrategy.getClass().getSimpleName());
		strategies.add(goldStrategy.getClass().getSimpleName());
		strategies.add(sp500Strategy.getClass().getSimpleName());
		strategies.add(betaStrategy.getClass().getSimpleName());
		strategies.add(highLanderStrategy.getClass().getSimpleName());

		strategies.removeAll(List.of(excludesStrategies));

		return strategies;
	}

	public List<Strategy> getStrategies(BarSeries series) {
		if (Objects.nonNull(this.strategies)) {
			return this.strategies;
		}
		List<Strategy> strategies = new ArrayList<Strategy>();
		strategies.add(adxStrategy.buildStrategy(series));
		strategies.add(runawayGAPStrategy.buildStrategy(series));
		strategies.add(cciCorrectionStrategy.buildStrategy(series));
		strategies.add(convergenceDivergenceStrategy.buildStrategy(series));
		strategies.add(engulfingStrategy.buildStrategy(series));
		strategies.add(globalExtremaStrategy.buildStrategy(series));
		strategies.add(haramiStrategy.buildStrategy(series));
		strategies.add(movingMomentumStrategy.buildStrategy(series));
		strategies.add(rsiStrategy.buildStrategy(series));
		strategies.add(simpleMovingMomentumStrategy.buildStrategy(series));
		strategies.add(threeBlackWhiteStrategy.buildStrategy(series));
		strategies.add(vwapStrategy.buildStrategy(series));
		strategies.add(emaTenTenStrategy.buildStrategy(series));
		strategies.add(hedgeIndexStrategy.buildStrategy(series));
		strategies.add(vixStrategy.buildStrategy());
		strategies.add(crudeOilStrategy.buildStrategy());
		strategies.add(goldStrategy.buildStrategy());
		strategies.add(sp500Strategy.buildStrategy());
		strategies.add(betaStrategy.buildStrategy(series));
		strategies.add(highLanderStrategy.buildStrategy(series));

		this.strategies = strategies;
		return strategies;
	}

	public Strategy getStrategyToRun(String strategy, BarSeries series) {
		if (Objects.isNull(this.strategies)) {
			this.strategies = getStrategies(series);
		}
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			return rsiStrategy.buildStrategy(series);
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			return movingMomentumStrategy.buildStrategy(series);
		} else if (strategy.equals(SimpleMovingMomentumStrategy.class.getSimpleName())) {
			return simpleMovingMomentumStrategy.buildStrategy(series);
		} else if (strategy.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			return globalExtremaStrategy.buildStrategy(series);
		} else if (strategy.equals(CCICorrectionStrategy.class.getSimpleName())) {
			return cciCorrectionStrategy.buildStrategy(series);
		} else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			return engulfingStrategy.buildStrategy(series);
		} else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			return haramiStrategy.buildStrategy(series);
		} else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			return threeBlackWhiteStrategy.buildStrategy(series);
		} else if (strategy.equals(ADXStrategy.class.getSimpleName())) {
			return adxStrategy.buildStrategy(series);
		} else if (strategy.equals(ConvergenceDivergenceStrategy.class.getSimpleName())) {
			return convergenceDivergenceStrategy.buildStrategy(series);
		} else if (strategy.equals(VWAPStrategy.class.getSimpleName())) {
			return vwapStrategy.buildStrategy(series);
		} else if (strategy.equals(RunawayGAPStrategy.class.getSimpleName())) {
			return runawayGAPStrategy.buildStrategy(series);
		} else if (strategy.equals(EMATenTwentyStrategy.class.getSimpleName())) {
			return emaTenTwentyStrategy.buildStrategy(series);
		} else if (strategy.equals(EMATenTenStrategy.class.getSimpleName())) {
			return emaTenTenStrategy.buildStrategy(series);
		} else if (strategy.equals(HedgeIndexStrategy.class.getSimpleName())) {
			return hedgeIndexStrategy.buildStrategy(series);
		} else if (strategy.equals(VIXStrategy.class.getSimpleName())) {
			return vixStrategy.buildStrategy();
		} else if (strategy.equals(CrudeOilStrategy.class.getSimpleName())) {
			return crudeOilStrategy.buildStrategy();
		} else if (strategy.equals(GoldStrategy.class.getSimpleName())) {
			return goldStrategy.buildStrategy();
		} else if (strategy.equals(SP500Strategy.class.getSimpleName())) {
			return sp500Strategy.buildStrategy();
		} else if (strategy.equals(BetaStrategy.class.getSimpleName())) {
			return betaStrategy.buildStrategy(series);
		} else if (strategy.equals(HighLanderStrategy.class.getSimpleName())) {
			return highLanderStrategy.buildStrategy(series);
		}
		else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}
	}

	public List<StrategyIndicatorValue> getIndicatorValues(String strategyName, BarSeries series) {
		if (strategyName.equals(RSI2Strategy.class.getSimpleName())) {
			return rsiStrategy.getIndicatorValues();
		} else if (strategyName.equals(SimpleMovingMomentumStrategy.class.getSimpleName())) {
			return simpleMovingMomentumStrategy.getIndicatorValues();
		} else if (strategyName.equals(MovingMomentumStrategy.class.getSimpleName())) {
			return movingMomentumStrategy.getIndicatorValues();
		} else if (strategyName.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			return globalExtremaStrategy.getIndicatorValues();
		} else if (strategyName.equals(CCICorrectionStrategy.class.getSimpleName())) {
			return cciCorrectionStrategy.getIndicatorValues();
		} else if (strategyName.equals(EngulfingStrategy.class.getSimpleName())) {
			return engulfingStrategy.getIndicatorValues();
		} else if (strategyName.equals(HaramiStrategy.class.getSimpleName())) {
			return haramiStrategy.getIndicatorValues();
		} else if (strategyName.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			return threeBlackWhiteStrategy.getIndicatorValues();
		} else if (strategyName.equals(ADXStrategy.class.getSimpleName())) {
			return adxStrategy.getIndicatorValues();
		} else if (strategyName.equals(ConvergenceDivergenceStrategy.class.getSimpleName())) {
			return convergenceDivergenceStrategy.getIndicatorValues();
		} else if (strategyName.equals(VWAPStrategy.class.getSimpleName())) {
			return vwapStrategy.getIndicatorValues();
		} else if (strategyName.equals(RunawayGAPStrategy.class.getSimpleName())) {
			return runawayGAPStrategy.getIndicatorValues();
		} else if (strategyName.equals(EMATenTwentyStrategy.class.getSimpleName())) {
			return emaTenTwentyStrategy.getIndicatorValues();
		} else if (strategyName.equals(EMATenTenStrategy.class.getSimpleName())) {
			return emaTenTenStrategy.getIndicatorValues();
		} else if (strategyName.equals(HedgeIndexStrategy.class.getSimpleName())) {
			return hedgeIndexStrategy.getIndicatorValues();
		} else if (strategyName.equals(VIXStrategy.class.getSimpleName())) {
			return vixStrategy.getIndicatorValues(); //TODO
		} else if (strategyName.equals(CrudeOilStrategy.class.getSimpleName())) {
			return crudeOilStrategy.getIndicatorValues(); //TODO
		} else if (strategyName.equals(GoldStrategy.class.getSimpleName())) {
			return goldStrategy.getIndicatorValues(); //TODO
		} else if (strategyName.equals(SP500Strategy.class.getSimpleName())) {
			return sp500Strategy.getIndicatorValues(); //TODO
		} else if (strategyName.equals(BetaStrategy.class.getSimpleName())) {
			return betaStrategy.getIndicatorValues(); //TODO
		} else if (strategyName.equals(HighLanderStrategy.class.getSimpleName())) {
			return highLanderStrategy.getIndicatorValues(); //TODO
		}
		else {
			throw new RuntimeException("Strategy not found!, strategyName="+strategyName);
		}

	}

}
