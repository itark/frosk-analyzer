package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyPerformance;
import nu.itark.frosk.repo.StrategyPerformanceRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.*;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.*;


import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.TradesRepository;
import org.ta4j.core.analysis.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.analysis.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.TradingStatement;

/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service
public class StrategyAnalysis {
	Logger logger = Logger.getLogger(StrategyAnalysis.class.getName());
	
	@Autowired
	BarSeriesService barSeriesService;
	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;
	
	@Autowired
	TradesRepository tradesRepository;
	
	@Autowired
	StrategyIndicatorValueRepository indicatorValueRepo;

	@Autowired
	StrategyPerformanceRepository strategyPerformanceRepository;
	
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
		if (Objects.isNull(strategy)  && Objects.isNull(security_id)) {
			List<String> strategies = StrategiesMap.buildStrategiesMap();
			strategies.forEach(strategyName -> {
				try {
					runStrategy(strategyName, barSeriesService.getDataSet());
				} catch (DataIntegrityViolationException e) {
					logger.severe("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
			
		} 
		else if (Objects.nonNull(strategy) && Objects.isNull(security_id)) {
			try {
				runStrategy(strategy, barSeriesService.getDataSet());
			} catch (Exception e) {
				logger.severe("Error runStrategy on strategy="+ strategy);
				throw e;
			}
		} 
		else if (Objects.nonNull(strategy) && security_id != null) {
			List<BarSeries> BarSeriesList = new ArrayList<BarSeries>();
			BarSeries BarSeries = barSeriesService.getDataSet(security_id);
			//Sanity check
			if (Objects.isNull(BarSeries) || BarSeries.isEmpty()) {
				throw new RuntimeException("BarSeries is null or empty. Download security prices.");
			}
			BarSeriesList.add(BarSeries);
			try {
				runStrategy(strategy, BarSeriesList);
			} catch (Exception e) {
				logger.severe("Error runStrategy on strategy="+ strategy+ " and security_id="+security_id);
				throw e;
			}
		} 
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		
	}
	public void runChooseBestStrategy() {
		for (BarSeries series : barSeriesService.getDataSet()) {
			setBestStrategy(series);
		}
	}

	private void runStrategy(String strategy, List<BarSeries> BarSeriesList) throws DataIntegrityViolationException{
		FeaturedStrategy fs = null;
		List<Position> positions = null;
        double totalProfit ;
        double totalProfitPercentage;
        Date latestTradeDate= null;
        Strategy strategyToRun = null;
        
		for (BarSeries series : BarSeriesList) {
			//logger.info("runStrategy("+strategy+", "+series.getName()+")");
			strategyToRun = getStrategyToRun(strategy, series);
			BarSeriesManager seriesManager = new BarSeriesManager(series);
			TradingRecord tradingRecord = seriesManager.run(strategyToRun);
			if (series.getBarData().isEmpty()){
				return;
			}
			Set<StrategyTrade> strategyTradeList = new HashSet<StrategyTrade>();
			StrategyTrade strategyTrade = null;

			for (Position position : tradingRecord.getPositions()) {
				//Entry
				Bar barEntry = series.getBar(position.getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = position.getEntry().getType().name();
				strategyTrade = new StrategyTrade(	buyDate,
													entryType,
													BigDecimal.valueOf(position.getEntry().getValue().doubleValue()),
													null,
													null);
				strategyTradeList.add(strategyTrade);
				// Exit
				Bar barExit = series.getBar(position.getExit().getIndex());
				Date sellDate = Date.from(barExit.getEndTime().toInstant());
				String exitType = position.getExit().getType().name();
				Num pnl = position.getProfit().dividedBy(position.getEntry().getValue()).multipliedBy(series.numOf(100));
				//logger.info("pnl="+pnl.doubleValue());
				strategyTrade = new StrategyTrade(	sellDate,
													exitType,
													BigDecimal.valueOf(position.getExit().getValue().doubleValue()),
													BigDecimal.valueOf(position.getProfit().doubleValue()),
													BigDecimal.valueOf(pnl.doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barExit.getEndTime().toInstant());
			}

			//If open
			if (tradingRecord.getCurrentPosition().isOpened()) {
				Bar barEntry = series.getBar(tradingRecord.getCurrentPosition().getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = tradingRecord.getCurrentPosition().getEntry().getType().name();
				strategyTrade = new StrategyTrade(	buyDate,
													entryType,
													BigDecimal.valueOf(tradingRecord.getCurrentPosition().getEntry().getValue().doubleValue()),
													null,
													null);
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barEntry.getBeginTime().toInstant());
			}

			fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, series.getName());
			if (fs == null) {  //=new run
				fs = new FeaturedStrategy();
				fs.setName(strategy);
				fs.setSecurityName(series.getName());
			}

			fs.setPeriod(getPeriod(series));
			fs.setLatestTrade(latestTradeDate);
			totalProfit = new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setTotalProfit(new BigDecimal(totalProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberofTrades(new BigDecimal(new NumberOfPositionsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double profitableTradesRatio = new WinningPositionsRatioCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(profitableTradesRatio)) {
				fs.setProfitableTradesRatio(new BigDecimal(profitableTradesRatio).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setTotalTransactionCost(
					new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord).doubleValue()));
			fs.setOpen(tradingRecord.getCurrentPosition().isOpened());

			FeaturedStrategy fsRes = featuredStrategyRepository.saveAndFlush(fs);
			//StrategyTrade
			List<StrategyTrade>  existingStrategyTrades = tradesRepository.findByFeaturedStrategyId(fsRes.getId());
			if (!existingStrategyTrades.isEmpty()) {
				existingStrategyTrades.forEach(st -> {
					tradesRepository.delete(st);
				});
			}
			strategyTradeList.forEach(st -> {
				st.setFeaturedStrategy(fsRes);
				tradesRepository.saveAndFlush(st);
			});
			//StrategyIndicatorValue
			List<StrategyIndicatorValue>  existIv = indicatorValueRepo.findByFeaturedStrategyId(fsRes.getId());
			if (!existIv.isEmpty()) {
				existIv.forEach(iv -> {
					indicatorValueRepo.delete(iv);
				});
			}
			getIndicatorValues(strategy, null).forEach(iv-> {
				iv.setFeaturedStrategy(fsRes);
				indicatorValueRepo.save(iv);
			});

			//logger.info("EXIT runStrategy("+strategy+", "+series.getName()+")");

		}
	}

	protected void setBestStrategy(BarSeries barSeries) {
		if (barSeries.getBarData().isEmpty()){
			return;
		}
		List<StrategyPerformance>  existSp = strategyPerformanceRepository.findBySecurityNameAndDate(barSeries.getName(),DateTimeManager.get(barSeries.getLastBar().getEndTime()));
		if (!existSp.isEmpty()) {
			existSp.forEach(sp -> {
				strategyPerformanceRepository.delete(sp);
			});
		}
		StrategyPerformance strategyPerformance = new StrategyPerformance();
		strategyPerformance.setDate(DateTimeManager.get(barSeries.getLastBar().getEndTime()));
		List<Strategy> strategies = StrategiesMap.getStrategies(barSeries);
		AnalysisCriterion profitCriterion = new GrossReturnCriterion();
		BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);
		BacktestExecutor backtestExecutor = new BacktestExecutor(barSeries);
		final List<TradingStatement> tradingStatements = backtestExecutor.execute(strategies, DoubleNum.valueOf(50), Trade.TradeType.BUY);
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
		strategyPerformance.setBestStrategy(bestStrategy.getName());
		Optional<TradingStatement> bestTradingStatement = tradingStatements.stream()
				.filter(s -> s.getStrategy().getName().equals(bestStrategy.getName())).findFirst();
		double pnl = bestTradingStatement.get().getPerformanceReport().getTotalProfitLossPercentage().doubleValue();
		strategyPerformance.setTotalProfitLoss(new BigDecimal(pnl).setScale(2, BigDecimal.ROUND_DOWN));
		strategyPerformance.setSecurityName(barSeries.getName());
		try {
			strategyPerformanceRepository.saveAndFlush(strategyPerformance);
		} catch (Exception e) {
			logger.severe("\nStrategyPerformance entity:"+ ReflectionToStringBuilder.toString(strategyPerformance));
			throw new RuntimeException(e);
		}
	}

	private List<StrategyIndicatorValue> getIndicatorValues(String strategy, BarSeries series) {
		if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
			RSI2Strategy strategyReguested = new RSI2Strategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(SimpleMovingMomentumStrategy.class.getSimpleName())) {
			SimpleMovingMomentumStrategy strategyReguested = new SimpleMovingMomentumStrategy(series);
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
		} else if (strategy.equals(EngulfingStrategy.class.getSimpleName())) {
			EngulfingStrategy strategyReguested = new EngulfingStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(HaramiStrategy.class.getSimpleName())) {
			HaramiStrategy strategyReguested = new HaramiStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(ThreeBlackWhiteStrategy.class.getSimpleName())) {
			ThreeBlackWhiteStrategy strategyReguested = new ThreeBlackWhiteStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}

	}
	
	private Strategy getStrategyToRun(String strategy,  BarSeries series) {
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
	
	private String getPeriod(BarSeries series) {
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

}
