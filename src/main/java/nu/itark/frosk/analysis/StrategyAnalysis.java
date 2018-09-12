package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.AverageProfitCriterion;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.BuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.LinearTransactionCostCriterion;
import org.ta4j.core.analysis.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.analysis.criteria.NumberOfBarsCriterion;
import org.ta4j.core.analysis.criteria.NumberOfTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import nu.itark.frosk.dataset.IndicatorValues;
import nu.itark.frosk.dataset.TradeView;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.service.TimeSeriesService;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;
/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service
public class StrategyAnalysis {
	Logger logger = Logger.getLogger(StrategyAnalysis.class.getName());
	
	@Autowired
	TimeSeriesService timeSeriesService;
	
	@Autowired
	FeaturedStrategyRepository fsRepo;
	
	/**
	 * This is the thing !!
	 * 
	 * <li>Analyse on all strategies and all securities</li>
	 * <li>Analyse on strategy and all securities, or</li>
	 * <li>Analyse on strategy and selected security, or</li>
	 * <li>Analyse on all strategies and selected security, or</li>
	 * 
	 * @param strategy can be null
	 * @param security can be null
	 * @return result in List<FeaturedStrategy>
	 */
	public List<FeaturedStrategyDTO> run(String strategy, String security) {
		logger.info("run("+strategy+", "+security+")");
	
		if (strategy == null && security == null) {
			return runStrategyMatrix();
		} 
		else if (strategy != null && security == null) {
			return runStrategy(strategy, timeSeriesService.getDataSet());
		} 
		else if (strategy != null && security != null) {
			List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>();
			timeSeriesList.add(timeSeriesService.getDataSet(security));
			return runStrategy(strategy, timeSeriesList);
		} 
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		
	}
	
	private List<FeaturedStrategyDTO> runStrategy(String strategy, List<TimeSeries> timeSeriesList) {
		List<FeaturedStrategyDTO> fsList = new ArrayList<FeaturedStrategyDTO>();
		FeaturedStrategyDTO fs = null;
		List<Trade> trades = null;
        double totalProfit ;
        double totalProfitPercentage;
        Date latestTradeDate= null;
        Strategy strategyToRun = null;
        List<IndicatorValues> indicatorValues = new ArrayList<IndicatorValues>();
        
		for (TimeSeries series : timeSeriesList) {
			strategyToRun = getStrategyToRun(strategy, series, indicatorValues);
			TimeSeriesManager seriesManager = new TimeSeriesManager(series);
			TradingRecord tradingRecord = seriesManager.run(strategyToRun);
			trades = tradingRecord.getTrades();
			List<TradeView> tradeViewList = new ArrayList<TradeView>();
			TradeView tr = null;

			for (Trade trade : trades) {
				Bar barEntry = series.getBar(trade.getEntry().getIndex());
				LocalDate buyDate = barEntry.getEndTime().toLocalDate();
				tr = new TradeView();
				tr.setDate(buyDate);
				tr.setType("B");
				tradeViewList.add(tr);

				Bar barExit = series.getBar(trade.getExit().getIndex());
				LocalDate sellDate = barExit.getEndTime().toLocalDate();
				tr = new TradeView();
				tr.setDate(sellDate);
				tr.setType("S");
				tradeViewList.add(tr);

				latestTradeDate = Date.from(barEntry.getEndTime().toInstant());

			}

			fs = new FeaturedStrategyDTO();
			fs.setName(strategy);
			fs.setSecurity(series.getName());
			fs.setPeriodDescription(getPeriod(series));
			fs.setLatestTradeDate(latestTradeDate);

			totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord).doubleValue();
			totalProfitPercentage = (totalProfit - 1) * 100;
			fs.setTotalProfit(new BigDecimal(totalProfitPercentage).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double profitableTradesRatio = new AverageProfitableTradesCriterion().calculate(series, tradingRecord).doubleValue();
			
			if (!Double.isNaN(profitableTradesRatio)) {
				fs.setProfitableTradesRatio(new BigDecimal(profitableTradesRatio).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
			double rewardRiskRatio = new RewardRiskRatioCriterion().calculate(series, tradingRecord).doubleValue();
			if (Double.isFinite(rewardRiskRatio)) {
				fs.setRewardRiskRatio(new BigDecimal(rewardRiskRatio).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double buyAndHold = new BuyAndHoldCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setBuyAndHold(new BigDecimal(buyAndHold).setScale(2, BigDecimal.ROUND_DOWN ));

			double totalProfitVsButAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion()).calculate(series, tradingRecord).doubleValue();
			fs.setTotalProfitVsButAndHold(new BigDecimal(totalProfitVsButAndHold).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setTotalTranactionCost(
					new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord).doubleValue()));
			fs.setTrades(tradeViewList);
			//fs.setIndicatorValues(indicatorValues);

			fsList.add(fs);
	
			save(fs);

		}

		return fsList;
	}

	private void save(FeaturedStrategyDTO dto) {
		FeaturedStrategy fs = fsRepo.findByNameAndSecurity(dto.getName(), dto.getSecurity());

		if (fs != null) { //Update
			fs.setTotalProfit(dto.getTotalProfit());
			fs.setAverageTickProfit(dto.getAverageTickProfit());
			fs.setNumberOfTicks(dto.getNumberOfTicks());
			fs.setAverageTickProfit(dto.getAverageTickProfit());
			fs.setNumberofTrades(dto.getNumberofTrades());
			fs.setProfitableTradesRatio(dto.getProfitableTradesRatio());
			fs.setMaxDD(dto.getMaxDD());
			fs.setRewardRiskRatio(dto.getRewardRiskRatio());
			fs.setTotalTransactionCost(dto.getTotalTranactionCost());
			fs.setBuyAndHold(dto.getBuyAndHold());
			fs.setTotalProfitVsButAndHold(dto.getTotalProfitVsButAndHold());
			fs.setPeriod(dto.getPeriodDescription());
			fs.setLatestTrade(dto.getLatestTradeDate());
		} else {  //New
			fs = get(dto);
		}

		fsRepo.save(fs);

	}
	
	private FeaturedStrategy get(FeaturedStrategyDTO dto) {
		return new FeaturedStrategy(dto.getName(), dto.getSecurity(), dto.getTotalProfit(), dto.getNumberOfTicks(), dto.getAverageTickProfit(), 
				dto.getNumberofTrades(), dto.getProfitableTradesRatio(), dto.getMaxDD(), dto.getRewardRiskRatio(), 
				dto.getTotalTranactionCost(), dto.getBuyAndHold(), dto.getTotalProfitVsButAndHold(), dto.getPeriodDescription(), dto.getLatestTradeDate());
		
	}
	
	private Strategy getStrategyToRun(String strategy,  TimeSeries series, List<IndicatorValues> indVals) {
		logger.info("getStrategyToRun("+strategy+")");
		Strategy strategyToRun = null;
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			RSI2Strategy strategyReguested = new RSI2Strategy(series);
			strategyToRun = strategyReguested.buildStrategy();
			indVals.addAll(strategyReguested.getIndicatorValues());
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			MovingMomentumStrategy strategyReguested = new MovingMomentumStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
			indVals.addAll(strategyReguested.getIndicatorValues());
		}
		if (strategyToRun == null) {
			throw new RuntimeException("strategyToRun is null");
		}

		logger.info("indVals set to="+indVals.size());
		
		return strategyToRun;
	}
	
	
	/**
	 * Load all TimeSeries defined by and run all available strategies.
	 * 
	 * @return List of FeaturedStrategy
	 */
	public List<FeaturedStrategyDTO> runStrategyMatrix() {
		logger.info("runStrategyMatrix()");
		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
		List<FeaturedStrategyDTO> fsList = new ArrayList<FeaturedStrategyDTO>();
		FeaturedStrategyDTO fs = null;
		List<Trade> trades = null;
		
        double totalProfit ;
        double totalProfitPercentage;
        Date latestTradeDate= null;

		for (TimeSeries series : timeSeriesList) {
			Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(series);  //TODO; Hardcoded it for now
			for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
				Strategy strategy = entry.getKey();
				String name = entry.getValue();
		        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
		        TradingRecord tradingRecord = seriesManager.run(strategy);
		        
		        trades = tradingRecord.getTrades();
		        if (trades.isEmpty()) continue;
		        
		        List<TradeView>  tradeViewList = new ArrayList<TradeView>();
		        TradeView tr = null;

		        for (Trade trade : trades) {
		            Bar barEntry = series.getBar(trade.getEntry().getIndex());
		            LocalDate buyDate = series.getBar(trade.getEntry().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(buyDate);
		            tr.setType("B");
		            tradeViewList.add(tr);
	            
		            LocalDate sellDate = series.getBar(trade.getExit().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(sellDate);
		            tr.setType("S");
		            tradeViewList.add(tr);
		            
					latestTradeDate = Date.from(barEntry.getEndTime().toInstant());
		            
		        }		        
		        
		        fs = new FeaturedStrategyDTO();
				fs.setName(name);
				fs.setSecurity(series.getName());
				fs.setPeriodDescription(getPeriod(series));
				fs.setLatestTradeDate(latestTradeDate);
				totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord).doubleValue();
				totalProfitPercentage = (totalProfit - 1) * 100;
				fs.setTotalProfit(new BigDecimal(totalProfitPercentage).setScale(2, BigDecimal.ROUND_DOWN));
				fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
				double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord).doubleValue();
				fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
				fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
				double profitableTradesRatio = new AverageProfitableTradesCriterion().calculate(series, tradingRecord).doubleValue();
				
				if (!Double.isNaN(profitableTradesRatio)) {
					fs.setProfitableTradesRatio(new BigDecimal(profitableTradesRatio).setScale(2, BigDecimal.ROUND_DOWN));
				}
				double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
				fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
				double rewardRiskRatio = new RewardRiskRatioCriterion().calculate(series, tradingRecord).doubleValue();
				if (Double.isFinite(rewardRiskRatio)) {
					fs.setRewardRiskRatio(new BigDecimal(rewardRiskRatio).setScale(2, BigDecimal.ROUND_DOWN));
				}
				double buyAndHold = new BuyAndHoldCriterion().calculate(series, tradingRecord).doubleValue();
				fs.setBuyAndHold(new BigDecimal(buyAndHold).setScale(2, BigDecimal.ROUND_DOWN ));

				double totalProfitVsButAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion()).calculate(series, tradingRecord).doubleValue();
				fs.setTotalProfitVsButAndHold(new BigDecimal(totalProfitVsButAndHold).setScale(2, BigDecimal.ROUND_DOWN));
				fs.setTotalTranactionCost(
						new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord).doubleValue()));
				fs.setTrades(tradeViewList);
				//fs.setIndicatorValues(indicatorValues);

				fsList.add(fs);
		
				save(fs);

			}
		}

		return fsList;
	}

	private String getPeriod(TimeSeries series) {
	StringBuilder sb = new StringBuilder();
    if (!series.getBarData().isEmpty()) {
        Bar firstBar = series.getFirstBar();
        Bar lastBar = series.getLastBar();
        sb.append(firstBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        sb.append("-");
        sb.append(lastBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    return sb.toString();
	}



//	public List<RNNPrices> getRNNPrices( String indiceName, Database database) {
//		List<RNNPrices> rnnPrices = new ArrayList<RNNPrices>();
//		RNNPrices rnnPrice;
//		pricePrediction.run();
//		double[] predicts = pricePrediction.getPredicts();
//		double[] actuals =pricePrediction.getActuals();
//
//		for (int i = 0; i < predicts.length; i++) {
//			rnnPrice = new RNNPrices();
//			rnnPrice.setPredicts(predicts[i]);
//			rnnPrice.setActuals(actuals[i]);
//			rnnPrices.add(rnnPrice);
//		}
//		
//		return rnnPrices;
//	}

}
