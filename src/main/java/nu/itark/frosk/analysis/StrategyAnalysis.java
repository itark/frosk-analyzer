package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import nu.itark.frosk.dataset.IndicatorValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
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

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.TradesRepository;
import nu.itark.frosk.service.TimeSeriesService;
import nu.itark.frosk.strategies.CCICorrectionStrategy;
import nu.itark.frosk.strategies.EngulfingStrategy;
import nu.itark.frosk.strategies.GlobalExtremaStrategy;
import nu.itark.frosk.strategies.HaramiStrategy;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;
import nu.itark.frosk.strategies.ThreeBlackWhiteStrategy;
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
	FeaturedStrategyRepository featuredStrategyRepository;
	
	@Autowired
	TradesRepository tradesRepository;
	
	@Autowired
	StrategyIndicatorValueRepository indicatorValueRepo;
	
	/**
	 * This is the thing !!
	 * 
	 * <li>Analyse on all strategies and all securities</li>
	 * <li>Analyse on strategy and all securities, or</li>
	 * <li>Analyse on strategy and selected security, or</li>
	 * <li>Analyse on all strategies and selected security, or</li>
	 * 
	 * @param strategy can be null
	 * @param security_id can be null
	 */
	public void run(String strategy, Long security_id) throws DataIntegrityViolationException {
		logger.info("run("+strategy+", "+security_id+")");
	
		if (strategy == null && security_id == null) {
			List<String> strategies = StrategiesMap.buildStrategiesMap();
			strategies.forEach(strategyName -> {
				try {
					runStrategy(strategyName, timeSeriesService.getDataSet());
				} catch (DataIntegrityViolationException e) {
					logger.severe("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
			
		} 
		else if (strategy != null && security_id == null) {
			try {
				runStrategy(strategy, timeSeriesService.getDataSet());
			} catch (Exception e) {
				logger.severe("Error runStrategy on strategy="+ strategy);
				throw e;
			}
		} 
		else if (strategy != null && security_id != null) {
			List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>();
			TimeSeries timeSeries = timeSeriesService.getDataSet(security_id);
			//Sanity check
			if (timeSeries == null || timeSeries.isEmpty()) {
				throw new RuntimeException("Timeseries is null or empty. Download security prices.");
			}
			timeSeriesList.add(timeSeries);
			try {
				runStrategy(strategy, timeSeriesList);
			} catch (Exception e) {
				logger.severe("Error runStrategy on strategy="+ strategy+ " and security_id="+security_id);
				throw e;
			}
		} 
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		
	}
	
	private void runStrategy(String strategy, List<TimeSeries> timeSeriesList) throws DataIntegrityViolationException{
		FeaturedStrategy fs = null;
		List<Trade> trades = null;
        double totalProfit ;
        double totalProfitPercentage;
        Date latestTradeDate= null;
        Strategy strategyToRun = null;
        
		for (TimeSeries series : timeSeriesList) {
			logger.info("runStrategy("+strategy+", "+series.getName()+")");
			strategyToRun = getStrategyToRun(strategy, series);
			TimeSeriesManager seriesManager = new TimeSeriesManager(series);
			TradingRecord tradingRecord = seriesManager.run(strategyToRun);
			trades = tradingRecord.getTrades();
//			logger.info(trades.size()+" trades found.");
			if (series.getBarData().isEmpty()){
				//abort
				return;
			}
			Set<StrategyTrade> strategyTradeList = new HashSet<StrategyTrade>();
			StrategyTrade strategyTrade = null;

			for (Trade trade : trades) {
				Bar barEntry = series.getBar(trade.getEntry().getIndex());
//		      	logger.info(series.getName()+"::barEntry="+barEntry.getDateName());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = trade.getEntry().getType().name();
				strategyTrade = new StrategyTrade(buyDate,entryType,BigDecimal.valueOf(barEntry.getMinPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);

				Bar barExit = series.getBar(trade.getExit().getIndex());
//			   	logger.info(series.getName()+"::barExit="+barExit.getDateName());
				Date sellDate = Date.from(barExit.getEndTime().toInstant());
				String exitType = trade.getExit().getType().name();
				strategyTrade = new StrategyTrade(sellDate,exitType,BigDecimal.valueOf(barExit.getMinPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barExit.getEndTime().toInstant());
			}

			fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, series.getName());
			if (fs == null) {  //=new run
				fs = new FeaturedStrategy();
				fs.setName(strategy);
				fs.setSecurityName(series.getName());
			} 
			
			fs.setPeriod(getPeriod(series));
			fs.setLatestTrade(latestTradeDate);
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
			if (Double.isFinite(buyAndHold)) {
				fs.setBuyAndHold(new BigDecimal(buyAndHold).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double totalProfitVsButAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion()).calculate(series, tradingRecord).doubleValue();

			if (Double.isFinite(totalProfitVsButAndHold)) {
				fs.setTotalProfitVsButAndHold(new BigDecimal(totalProfitVsButAndHold).setScale(2, BigDecimal.ROUND_DOWN));
			}
			fs.setTotalTransactionCost(
					new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord).doubleValue()));

			FeaturedStrategy fsRes = featuredStrategyRepository.saveAndFlush(fs);
			//Trades
			List<StrategyTrade>  existSt = tradesRepository.findByFeaturedStrategyId(fsRes.getId());
			if (existSt.isEmpty()) {
				strategyTradeList.forEach(st -> {
					st.setFeaturedStrategy(fsRes);
					tradesRepository.saveAndFlush(st);
				});
			}
		}

	}

	//TODO implement IndicatorValue
	public List<IndicatorValue> getIndicatorValues(String strategy, TimeSeries series) {
//		logger.info("getIndicatorValues("+strategy+", "+series.getName()+")");

		Strategy strategyToRun = null;
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			RSI2Strategy strategyReguested = new RSI2Strategy(series);
			strategyToRun = strategyReguested.buildStrategy();
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			MovingMomentumStrategy strategyReguested = new MovingMomentumStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			GlobalExtremaStrategy strategyReguested = new GlobalExtremaStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(CCICorrectionStrategy.class.getSimpleName())) {
			CCICorrectionStrategy strategyReguested = new CCICorrectionStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			EngulfingStrategy strategyReguested = new EngulfingStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			HaramiStrategy strategyReguested = new HaramiStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			ThreeBlackWhiteStrategy strategyReguested = new ThreeBlackWhiteStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();
			return strategyReguested.getIndicatorValues();
		}
		
		if (strategyToRun == null) {
			throw new RuntimeException("strategyToRun is null");
		}		

		return null;
		
	}
	
	
	public Strategy getStrategyToRun(String strategy,  TimeSeries series) {
//		logger.info("getStrategyToRun("+strategy+", "+series.getName());
		Strategy strategyToRun = null;
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			RSI2Strategy strategyReguested = new RSI2Strategy(series);
			strategyToRun = strategyReguested.buildStrategy();
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			MovingMomentumStrategy strategyReguested = new MovingMomentumStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();	
		} else if (strategy.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			GlobalExtremaStrategy strategyReguested = new GlobalExtremaStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(CCICorrectionStrategy.class.getSimpleName())) {
			CCICorrectionStrategy strategyReguested = new CCICorrectionStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			EngulfingStrategy strategyReguested = new EngulfingStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			HaramiStrategy strategyReguested = new HaramiStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		} else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			ThreeBlackWhiteStrategy strategyReguested = new ThreeBlackWhiteStrategy(series);
			strategyToRun = strategyReguested.buildStrategy();		
		}
		
		else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}
		
		if (strategyToRun == null) {
			throw new RuntimeException("strategyToRun is null");
		}

		
		return strategyToRun;
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


	public List<StrategyTrade> getOpenTrades(String name) {
		List<StrategyTrade> strategyTradeListList = new ArrayList<>();
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(name);
			fsList.forEach(featuredStrategy -> {
				final List<StrategyTrade> byFeaturedStrategy = tradesRepository.findByFeaturedStrategy(featuredStrategy);
				if (byFeaturedStrategy.isEmpty()) return;
				StrategyTrade latestTrade = Collections.max(byFeaturedStrategy, Comparator.comparing(StrategyTrade::getDate));
				if (latestTrade.getType().equals(Order.OrderType.BUY.name())) {
					strategyTradeListList.add(latestTrade);
				}
			});
		return strategyTradeListList;
	}

	public List<StrategyTrade> getOpenTrades() {
		List<StrategyTrade> strategyTradeListList = new ArrayList<>();
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
		fsList.forEach(featuredStrategy -> {
			final List<StrategyTrade> byFeaturedStrategy = tradesRepository.findByFeaturedStrategy(featuredStrategy);
			if (byFeaturedStrategy.isEmpty()) return;
			StrategyTrade latestTrade = Collections.max(byFeaturedStrategy, Comparator.comparing(StrategyTrade::getDate));
			if (latestTrade.getType().equals(Order.OrderType.BUY.name())) {
				strategyTradeListList.add(latestTrade);
			}
		});
		return strategyTradeListList;
	}

	public List<StrategyTrade> getTrades(String security, String strategy){
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security);
		return tradesRepository.findByFeaturedStrategy(fs);
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
