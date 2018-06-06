package nu.itark.frosk.service;

import java.util.List;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
//import nu.itark.frosk.analysis.dl4j.recurrent.RNNPrices;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.dataset.TradeView;

@Deprecated
public interface FeaturedStrategyServiceOBSOLETE {


	/**
	 * Return all Featured Strategies
	 * 
	 * @return
	 */
	public List<FeaturedStrategyDTO> getAllFeaturedStrategies(Database database);
	

	/**
	 * Return Trades per stratergy and indice
	 * @param strategyName 
	 * 
	 * @return
	 */
	public List<TradeView> getTrades(String indiceName, String strategyName, Database database);

	/**
	 * Return strategy for actual database.
	 * 
	 * @param strategy
	 * @param database
	 * @return
	 */
	public List<FeaturedStrategyDTO> getFeaturedStrategy(String strategy, Database database);
	
	
	/**
	 * Return RNN prices.
	 * 
	 * @param indiceName
	 * @param strategyName
	 * @param database
	 * @return
	 */
//	public List<RNNPrices> getRNNPrices(String indiceName, Database database);	
	
	
}
