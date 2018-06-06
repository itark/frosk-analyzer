package nu.itark.frosk.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.TradeView;

@Service
public class FeaturedStrategyService {

	@Autowired
	private StrategyAnalysis strategyAnalysis;


	public List<FeaturedStrategyDTO> getAllFeaturedStrategies() {
		return strategyAnalysis.runStrategyMatrix();
	}


	public List<TradeView> getTrades(String strategyName, String indiceName) {
		return strategyAnalysis.getTrades(strategyName, indiceName);
	}


	public List<FeaturedStrategyDTO> getFeaturedStrategy(String strategy) {
		return strategyAnalysis.runStrategyMatrix();
	}


//	public List<RNNPrices> getRNNPrices(String indiceName, Database database) {
//		return strategyAnalysis.getRNNPrices( indiceName, database);
//	}

}
