package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.bot.TradingBot;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyPerformance;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.StrategyPerformanceRepository;
import nu.itark.frosk.repo.TradesRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.*;
import nu.itark.frosk.util.DateTimeManager;
import nu.itark.frosk.util.FroskUtil;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.TradingStatement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service
@Slf4j
public class StrategyAnalysis {

	@Value("${exchange.transaction.initialAmount}")
	private double initialAmount;

	@Value("${exchange.transaction.feePerTradePercent}")
	private double feePerTradePercent;

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

	@Autowired
	TradingBot tradingBot;

	@Autowired
	Costs costs;
	
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
					log.error("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
			
		} 
		else if (Objects.nonNull(strategy) && Objects.isNull(security_id)) {
			try {
				runStrategy(strategy, barSeriesService.getDataSet());
			} catch (Exception e) {
				log.error("Error runStrategy on strategy="+ strategy);
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
				log.error("Error runStrategy on strategy="+ strategy+ " and security_id="+security_id);
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

	public void runBot(String strategy, BarSeries barSeries) {
		Strategy strategyToRun = null;
		strategyToRun = getStrategyToRun(strategy, barSeries);
		tradingBot.run(strategyToRun, barSeries);
	}

	private void runStrategy(String strategy, List<BarSeries> barSeriesList) throws DataIntegrityViolationException{
		FeaturedStrategy fs = null;
        double totalProfit ;
        Date latestTradeDate= null;
        Strategy strategyToRun;

		for (BarSeries series : barSeriesList) {
			log.info("runStrategy("+strategy+", "+series.getName()+")");
			strategyToRun = getStrategyToRun(strategy, series);
			BarSeriesManager seriesManager = new BarSeriesManager(series, costs.getTransactionCostModel(),  costs.getBorrowingCostModel());
			TradingRecord tradingRecord = seriesManager.run(strategyToRun);
			if (series.getBarData().isEmpty()){
				log.warn("Something fishy on {}. BarData isEmpty, continues...", series.getName());
				continue;
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
				if (pnl.isNaN()) {
					log.info("series.getName()="+series.getName());
					log.info("position.getExit().getValue().doubleValue()="+position.getExit().getValue().doubleValue());
					log.info("position.getProfit().doubleValue()="+position.getProfit().doubleValue());
					pnl = series.numOf(0);
					log.info("pnl="+pnl);

				}
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
			//totalProfit = new ReturnCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(totalProfit)) {
				fs.setTotalProfit(new BigDecimal(totalProfit).setScale(2, BigDecimal.ROUND_DOWN));
			} else {
				fs.setTotalProfit(BigDecimal.ZERO);
			}
			//Hidden in UI
			//fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			//double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord).doubleValue();
			//Hidden in UI
			//fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberofTrades(new BigDecimal(new NumberOfPositionsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double profitableTradesRatio =  PositionsRatioCriterion.WinningPositionsRatioCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(profitableTradesRatio)) {
				fs.setProfitableTradesRatio(new BigDecimal(profitableTradesRatio).setScale(2, RoundingMode.DOWN));
			}
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(maximumDrawdownCriterion)) {
				fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, RoundingMode.DOWN));
			} else {
				fs.setMaxDD(BigDecimal.ZERO);
			}
			double totalTransactionCost = new LinearTransactionCostCriterion(initialAmount, feePerTradePercent).calculate(series, tradingRecord).doubleValue();
			if(!Double.isNaN(totalTransactionCost)) {
				fs.setTotalTransactionCost(new BigDecimal(totalTransactionCost));
			} else {
				fs.setTotalTransactionCost(BigDecimal.ZERO);
			}
			fs.setOpen(tradingRecord.getCurrentPosition().isOpened());
			fs.setSqn(new BigDecimal(new SqnCriterion().calculate(series, tradingRecord).doubleValue()));
			fs.setExpectency(new BigDecimal(new ExpectancyCriterion().calculate(series, tradingRecord).doubleValue()));
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

			log.info("EXIT runStrategy("+strategy+", "+series.getName()+")");

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
		List<Strategy> strategies = StrategiesMap.getStrategies(barSeries);
		AnalysisCriterion profitCriterion = new ReturnCriterion();
		BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);
		BacktestExecutor backtestExecutor = new BacktestExecutor(barSeries);
		final List<TradingStatement> tradingStatements = backtestExecutor.execute(strategies, DoubleNum.valueOf(50), Trade.TradeType.BUY);
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));

		StrategyPerformance strategyPerformance = new StrategyPerformance();
		strategyPerformance.setDate(DateTimeManager.get(barSeries.getLastBar().getEndTime()));
		strategyPerformance.setBestStrategy(bestStrategy.getName());
		strategyPerformance.setTotalProfitLoss(getTotalPnL(tradingStatements, bestStrategy));
		strategyPerformance.setSecurityName(barSeries.getName());

		try {
			strategyPerformanceRepository.saveAndFlush(strategyPerformance);
		} catch (Exception e) {
			log.error("\nStrategyPerformance entity:"+ ReflectionToStringBuilder.toString(strategyPerformance));
			throw new RuntimeException(e);
		}
	}

	private static BigDecimal getTotalPnL(List<TradingStatement> tradingStatements, Strategy bestStrategy) {
		Optional<TradingStatement> bestTradingStatement = tradingStatements.stream()
				.filter(s -> s.getStrategy().getName().equals(bestStrategy.getName())).findFirst();
		double pnl = bestTradingStatement.get().getPerformanceReport().getTotalProfitLossPercentage().doubleValue();
		return new BigDecimal(pnl).setScale(2, BigDecimal.ROUND_DOWN);
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
		} else if (strategy.equals(ADXStrategy.class.getSimpleName())) {
			ADXStrategy strategyReguested = new ADXStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(ConvergenceDivergenceStrategy.class.getSimpleName())) {
			ConvergenceDivergenceStrategy strategyReguested = new ConvergenceDivergenceStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(VWAPStrategy.class.getSimpleName())) {
			VWAPStrategy strategyReguested = new VWAPStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(RunawayGAPStrategy.class.getSimpleName())) {
			RunawayGAPStrategy strategyReguested = new RunawayGAPStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else if (strategy.equals(EMATenTwentyStrategy.class.getSimpleName())) {
			EMATenTwentyStrategy strategyReguested = new EMATenTwentyStrategy(series);
			return strategyReguested.getIndicatorValues();
		} else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}

	}


	private Strategy getStrategyToRun(String strategy, BarSeries series) {
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
		} else if (strategy.equals(ADXStrategy.class.getSimpleName())) {
			return new ADXStrategy(series).buildStrategy();
		} else if (strategy.equals(ConvergenceDivergenceStrategy.class.getSimpleName())) {
			return new ConvergenceDivergenceStrategy(series).buildStrategy();
		} else if (strategy.equals(VWAPStrategy.class.getSimpleName())) {
			return new VWAPStrategy(series).buildStrategy();
		} else if (strategy.equals(RunawayGAPStrategy.class.getSimpleName())) {
			return new RunawayGAPStrategy(series).buildStrategy();
		} else if (strategy.equals(EMATenTwentyStrategy.class.getSimpleName())) {
			return new EMATenTwentyStrategy(series).buildStrategy();
		} else {
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
