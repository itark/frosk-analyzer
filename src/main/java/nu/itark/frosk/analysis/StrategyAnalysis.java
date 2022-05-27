package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import nu.itark.frosk.dataset.IndicatorValue;
import nu.itark.frosk.strategies.*;
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
			if (series.getBarData().isEmpty()){
				//abort
				return;
			}
			Set<StrategyTrade> strategyTradeList = new HashSet<StrategyTrade>();
			StrategyTrade strategyTrade = null;

			for (Trade trade : trades) {
				Bar barEntry = series.getBar(trade.getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = trade.getEntry().getType().name();
				strategyTrade = new StrategyTrade(buyDate,entryType,BigDecimal.valueOf(barEntry.getMinPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);

				Bar barExit = series.getBar(trade.getExit().getIndex());
				Date sellDate = Date.from(barExit.getEndTime().toInstant());
				String exitType = trade.getExit().getType().name();
				strategyTrade = new StrategyTrade(sellDate,exitType,BigDecimal.valueOf(barExit.getMinPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barExit.getEndTime().toInstant());
			}

			//If open
			if (tradingRecord.getCurrentTrade().isOpened()) {
				Bar barEntry = series.getBar(tradingRecord.getCurrentTrade().getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = tradingRecord.getCurrentTrade().getEntry().getType().name();
				strategyTrade = new StrategyTrade(buyDate,entryType,BigDecimal.valueOf(barEntry.getMinPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);
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

	public List<IndicatorValue> getIndicatorValues(String strategy, TimeSeries series) {
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			RSI2Strategy strategyReguested = new RSI2Strategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			MovingMomentumStrategy strategyReguested = new MovingMomentumStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			GlobalExtremaStrategy strategyReguested = new GlobalExtremaStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(CCICorrectionStrategy.class.getSimpleName())) {
			CCICorrectionStrategy strategyReguested = new CCICorrectionStrategy(series);
			return strategyReguested.getIndicatorValues();
		}
		/*
		else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			EngulfingStrategy strategyReguested = new EngulfingStrategy(series);
			return strategyReguested.getIndicatorValues();
		}
		*/
		/*
		else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			HaramiStrategy strategyReguested = new HaramiStrategy(series);
			return strategyReguested.getIndicatorValues();
		}*/
		else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			ThreeBlackWhiteStrategy strategyReguested = new ThreeBlackWhiteStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}

	}
	
	public Strategy getStrategyToRun(String strategy,  TimeSeries series) {
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			return new RSI2Strategy(series).buildStrategy();
		} else if (strategy.equals(MovingMomentumStrategy.class.getSimpleName())) {
			return new MovingMomentumStrategy(series).buildStrategy();
		} else if (strategy.equals(SimpleMovingMomentumStrategy.class.getSimpleName())) {
			return new SimpleMovingMomentumStrategy(series).buildStrategy();
		} else if (strategy.equals(GlobalExtremaStrategy.class.getSimpleName())) {
			return new GlobalExtremaStrategy(series).buildStrategy();
		} else if (strategy.equals(CCICorrectionStrategy.class.getSimpleName())) {
			return new CCICorrectionStrategy(series).buildStrategy();
		} else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			return new EngulfingStrategy(series).buildStrategy();
		} else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			return new HaramiStrategy(series).buildStrategy();
		} else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			return new ThreeBlackWhiteStrategy(series).buildStrategy();
		}
		else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}

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


	public List<StrategyTrade> getLongTradesAllStrategies(String strategyName) {
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
		return getTradesForStrategies(fsList, Order.OrderType.BUY);
	}

	public List<StrategyTrade> getShortTrades(String strategyName) {
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
		return getTradesForStrategies(fsList, Order.OrderType.SELL);
	}

	public List<StrategyTrade> getLongTradesAllStrategies() {
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
		return getTradesForStrategies(fsList, Order.OrderType.BUY);
	}

	public List<StrategyTrade> getShortTradesAllStrategies() {
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
		return getTradesForStrategies(fsList, Order.OrderType.SELL);
	}

	private List<StrategyTrade> getTradesForStrategies(List<FeaturedStrategy> fsList, Order.OrderType orderType) {
		List<StrategyTrade> strategyTradeListList = new ArrayList<>();
		fsList.forEach(featuredStrategy -> {
			final List<StrategyTrade> byFeaturedStrategy = tradesRepository.findByFeaturedStrategy(featuredStrategy);
			if (byFeaturedStrategy.isEmpty()) return;
			StrategyTrade latestTrade = Collections.max(byFeaturedStrategy, Comparator.comparing(StrategyTrade::getDate));
			if (latestTrade.getType().equals(orderType.name())) {
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
