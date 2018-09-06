package nu.itark.frosk.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Strategy;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;

@Service
public class FeaturedStrategyService {

//	@Autowired
//	private StrategyAnalysis strategyAnalysis;


//	public List<FeaturedStrategyDTO> getAllFeaturedStrategies() {
//		return strategyAnalysis.runStrategyMatrix();
//	}


//	public List<TradeView> getTrades(String strategyName, String indiceName) {
//		return strategyAnalysis.getTrades(strategyName, indiceName);
//	}


//	public List<FeaturedStrategyDTO> getFeaturedStrategy(String strategy, String security) {
//        Strategy strategyToRun = null;
//		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
//			RSI2Strategy strategyReguested = new RSI2Strategy(series);
//			strategyToRun = strategyReguested.buildStrategy();
//		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
//			MovingMomentumStrategy strategyReguested = new MovingMomentumStrategy(series);
//			strategyToRun = strategyReguested.buildStrategy();				
//		}
//		if (strategyToRun == null) {
//			throw new RuntimeException("strategyToRun is null");
//		}
//	
//		
//		return strategyAnalysis.runStrategy(strategy);
//	}	
	
	
//	
//	public List<FeaturedStrategyDTO> getFeaturedStrategy(String strategy) {
////		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
////			RSI2Strategy strat = new RSI2Strategy();
////			strat.buildStrategy(series)
////		}
//		
//		return strategyAnalysis.runStrategy(strategy);
//	}


//	public List<RNNPrices> getRNNPrices(String indiceName, Database database) {
//		return strategyAnalysis.getRNNPrices( indiceName, database);
//	}

}
