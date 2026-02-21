package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.bot.TradingBot;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.dataset.YAHOODataManager;
import nu.itark.frosk.model.*;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.hedge.*;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BacktestExecutor;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.TradingStatement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service
@Slf4j
public class StrategyAnalysis {

	@Value("${exchange.transaction.feePerTradePercent}")
	private double feePerTradePercent;

	@Value("${frosk.database.only:YAHOO}")
	private String databaseOnly;

	@Value("${frosk.strategy.only}")
	private String strategyOnly;

	@Value("${frosk.strategies.hedge.strategy}")
	private String[] excludeHedgeStrategies;

	@Autowired
	BarSeriesService barSeriesService;

	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;

	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
    StrategyTradeRepository tradesRepository;
	
	@Autowired
	StrategyIndicatorValueRepository indicatorValueRepo;

	@Autowired
	StrategyPerformanceRepository strategyPerformanceRepository;

	@Autowired
	StrategiesMap strategiesMap;

	@Autowired
	TradingBot tradingBot;

	@Autowired
	HedgeIndexService hedgeIndexService;

	@Autowired
	YAHOODataManager yahooDataManager;


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
	@Transactional
	public void runOLD(String strategy, Long security_id) throws DataIntegrityViolationException {
		log.info("run("+strategy+","+security_id+")");
		if (Objects.isNull(strategy)  && Objects.isNull(security_id)) {
				List<String> strategies = strategiesMap.buildStrategiesMap();
			strategies.removeAll(List.of(excludeHedgeStrategies));
			if (strategyOnly != null && !strategyOnly.isEmpty()) {
				strategies = List.of(strategyOnly);
			}
			log.info("{} strategies to run. ",strategies.size());
			strategies.forEach(strategyName -> {
				try {
					runStrategy(strategyName, barSeriesService.getDataSet(Database.valueOf(databaseOnly)));
				} catch (DataIntegrityViolationException e) {
					log.error("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
		}
		else if (Objects.isNull(strategy)  && Objects.nonNull(security_id)) {
			List<String> strategies = strategiesMap.buildStrategiesMap();
			strategies.removeAll(List.of(excludeHedgeStrategies));
			if (strategyOnly != null && !strategyOnly.isEmpty()) {
				strategies = List.of(strategyOnly);
			}
			strategies.forEach(strategyName -> {
				try {
					runStrategy(strategyName, List.of(barSeriesService.getDataSet(security_id)));
				} catch (DataIntegrityViolationException e) {
					log.error("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
		} else if (Objects.nonNull(strategy) && Objects.isNull(security_id)) {
			try {
				runStrategy(strategy, barSeriesService.getDataSet(Database.valueOf(databaseOnly)));
			} catch (Exception e) {
				log.error("Error runStrategy on strategy="+ strategy);
				throw e;
			}
		} else if (Objects.nonNull(strategy) && security_id != null) {
			List<BarSeries> barSeriesList = new ArrayList<BarSeries>();
			BarSeries BarSeries = barSeriesService.getDataSet(security_id);
			//Sanity check
			if (Objects.isNull(BarSeries) || BarSeries.isEmpty()) {
				throw new RuntimeException("BarSeries is null or empty. Download security prices.");
			}
			barSeriesList.add(BarSeries);
			try {
				runStrategy(strategy, barSeriesList);
			} catch (Exception e) {
				log.error("Error runStrategy on strategy="+ strategy+ " and security_id="+security_id);
				throw e;
			}
		}
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		log.info("run("+strategy+","+security_id+") READY");
		
	}


	public void run(String strategy, Long security_id) throws DataIntegrityViolationException {
		log.info("run({},{})", strategy, security_id);

		try {
			List<String> strategies = getStrategiesToRun(strategy);
			List<BarSeries> dataset = getDataset(security_id);

			if (strategy == null) {
				strategies.forEach(strategyName -> runStrategyWithErrorHandling(strategyName, dataset));
			} else {
				validateDataset(dataset, security_id);
				runStrategyWithErrorHandling(strategy, dataset);
			}
		}
		catch (IndexOutOfBoundsException e) {
			log.error("IndexOutOfBoundsException running strategy={}, security_id={}. This may be a ta4j library bug with indicator calculation at series boundaries.",
					strategy, security_id, e);
			throw new DataIntegrityViolationException("Strategy execution failed due to index out of bounds", e);
		}

		log.info("run({},{}) READY", strategy, security_id);
	}

	private List<String> getStrategiesToRun(String strategy) {
		if (strategy != null) {
			return List.of(strategy);
		}

		if (strategyOnly != null && !strategyOnly.isEmpty()) {
			return List.of(strategyOnly);
		}

		List<String> strategies = strategiesMap.buildStrategiesMap();
		strategies.removeAll(List.of(excludeHedgeStrategies));
		log.info("{} strategies to run.", strategies.size());
		return strategies;
	}

	private List<BarSeries> getDataset(Long security_id) {
		if (security_id != null) {
			return List.of(barSeriesService.getDataSet(security_id));
		}
		return barSeriesService.getDataSet(Database.valueOf(databaseOnly));
	}

	private void validateDataset(List<BarSeries> dataset, Long security_id) {
		if (security_id != null && (dataset == null || dataset.isEmpty() || dataset.get(0).isEmpty())) {
			throw new RuntimeException("BarSeries is null or empty. Download security prices.");
		}
	}

	private void runStrategyWithErrorHandling(String strategyName, List<BarSeries> dataset) {
		try {
			runStrategy(strategyName, dataset);
		} catch (IndexOutOfBoundsException e) {
			log.error("IndexOutOfBoundsException in strategy={} - possibly a ta4j indicator bug accessing series boundaries, continues...",
					strategyName, e);
		}
		catch (DataIntegrityViolationException e) {
			log.error("Error runStrategy on strategyName={}", strategyName);
			throw e;
		} catch (Exception e) {
			log.error("Error runStrategy on strategy={}", strategyName);
			throw e;
		}
	}

	public void runChooseBestStrategy() {
		for (BarSeries series : barSeriesService.getDataSet(Database.valueOf(databaseOnly))) {
			setBestStrategy(series);
		}
	}

	public void runBot(String strategy, Long security_id) {
		Strategy strategyToRun = null;
		BarSeries barSeries = barSeriesService.getDataSet(security_id);
		strategyToRun = strategiesMap.getStrategyToRun(strategy, barSeries);
		tradingBot.runningPositions(strategyToRun, barSeries);
	}

	public void runningPositions() {
		tradingBot.runningPositions();
	}

	public void runHedgeIndexStrategies() {
		log.info("runHedgeIndexStrategies()");
		runVix();
		runVVix();
		runCrudeOil();
		runGold();
		runSP500();
		runNasdaqVsSP();
		//TODO: FX USD/JPY Rising above 150
		//TODO: FX AUD/USD Drops >2% in last 5 days
		//TODO: FX DXY Above 105 and rising
		//TODO: Inflation US CPI YoY CPI > 3.5% and rising
		//TODO: Inflation Core CPI MoM > 0.4%
		//TODO: Interest Rates 10Y Treasury Yield > 4.5% and rising
		//TODO: Yield Curve 2Y - 10Y Spread < -50 bps (deep inversion)
		hedgeIndexService.update();
		log.info("runHedgeIndexStrategies executed");
	}

	private void runVix() {
		String securityName = "^VIX";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId(securityName);
		run(VIXStrategy.class.getSimpleName(), sec_id);
	}

	private void runVVix() {
		String securityName = "^VVIX";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId(securityName);
		run(VVIXStrategy.class.getSimpleName(), sec_id);
	}

	private void runCrudeOil() {
		String securityName = "CL=F";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId(securityName);
		run(CrudeOilStrategy.class.getSimpleName(), sec_id);
	}

	private void runGold() {
		String securityName = "GC=F";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId(securityName);
		run(GoldStrategy.class.getSimpleName(), sec_id);
	}

	private void runSP500() {
		String securityName = "^GSPC";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId(securityName);
		run(SP500Strategy.class.getSimpleName(), sec_id);
	}

	private void runNasdaqVsSP() {
		String securityName = "^IXIC";
		yahooDataManager.syncronize(securityName);
		Long sec_id = barSeriesService.getSecurityId("^IXIC"	);
		run(NasdaqVsSPStrategy.class.getSimpleName(), sec_id);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
//	@Async
	private void runStrategy(String strategy, List<BarSeries> barSeriesList) throws DataIntegrityViolationException{
		AtomicReference<FeaturedStrategy> fs = new AtomicReference<>();
        AtomicReference<Double> totalProfit = new AtomicReference<>((double) 0);
		AtomicReference<Double> totalGrossReturn = new AtomicReference<>((double) 0);
        AtomicReference<Date> latestTradeDate= new AtomicReference<>();
        AtomicReference<Strategy> strategyToRun =  new AtomicReference<>();

		barSeriesList.stream().forEach(series -> {

		//barSeriesList.parallelStream().forEach(series -> {
			//log.info("runStrategy("+strategy+", "+series.getName()+")");
            try {
                strategyToRun.set(strategiesMap.getStrategyToRun(strategy, series));
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("continue on: "+e);
            }
            Security security = securityRepository.findById(Long.valueOf(series.getName())).orElse(null);
			//log.info("Running strategy {} on security {} - {}", strategy, security.getId(), security.getName());
			if (series.getBarData().isEmpty()){
				log.warn("Something fishy on {}. BarData isEmpty, continues...", series.getName());
				security.setActive(false);
				securityRepository.save(security);
				return;
			}
			fs.set(featuredStrategyRepository.findByNameAndSecurityName(strategy, security.getName()));
			if (fs.get() == null) {
				fs.set(new FeaturedStrategy());
				fs.get().setName(strategy);
				fs.get().setSecurityName(security.getName());
				fs.get().setSecurityDesc(security.getDescription());
			}
			// Running the strategy
			TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategyToRun.get());
			Set<StrategyTrade> strategyTradeList = new HashSet<StrategyTrade>();
			StrategyTrade strategyTrade = null;
			for (Position position : tradingRecord.getPositions()) {
				//Entry
				Bar barEntry = series.getBar(position.getEntry().getIndex());
				strategyTrade = new StrategyTrade();
				strategyTrade.setDate(Date.from(barEntry.getEndTime().toInstant()));
				strategyTrade.setType(position.getEntry().getType().name());
				strategyTrade.setPrice(BigDecimal.valueOf(position.getEntry().getPricePerAsset().doubleValue()));
				strategyTrade.setAmount(BigDecimal.valueOf(position.getEntry().getAmount().doubleValue()));
				strategyTradeList.add(strategyTrade);
				// Exit
				Bar barExit = series.getBar(position.getExit().getIndex());
				Date sellDate = Date.from(barExit.getEndTime().toInstant());
				String exitType = position.getExit().getType().name();
				BigDecimal grossProfit = BigDecimal.valueOf(position.getGrossProfit().doubleValue());
				BigDecimal pnl = BigDecimal.ZERO;
				if (!Double.isNaN(position.getGrossReturn().doubleValue())) {
					pnl = new BigDecimal((position.getGrossReturn().doubleValue()-1)*100).setScale(4, BigDecimal.ROUND_DOWN);
				}
				strategyTrade = new StrategyTrade();
				strategyTrade.setDate(sellDate);
				strategyTrade.setType(exitType);
				strategyTrade.setPrice(BigDecimal.valueOf(position.getExit().getPricePerAsset().doubleValue()));
				strategyTrade.setAmount(BigDecimal.valueOf(position.getEntry().getAmount().doubleValue()));
				strategyTrade.setGrossProfit( grossProfit  );
				strategyTrade.setPnl(pnl);
				strategyTrade.setFeaturedStrategy(fs.get());
				strategyTradeList.add(strategyTrade);
				latestTradeDate.set(Date.from(barExit.getEndTime().toInstant()));
			}
			//If open
			if (tradingRecord.getCurrentPosition().isOpened()) {
				Bar barEntry = series.getBar(tradingRecord.getCurrentPosition().getEntry().getIndex());
				strategyTrade = new StrategyTrade();
				strategyTrade.setDate(Date.from(barEntry.getEndTime().toInstant()));
				strategyTrade.setType(tradingRecord.getCurrentPosition().getEntry().getType().name());
				strategyTrade.setPrice(BigDecimal.valueOf(tradingRecord.getCurrentPosition().getEntry().getPricePerAsset().doubleValue()));
				strategyTrade.setAmount(BigDecimal.valueOf(tradingRecord.getCurrentPosition().getEntry().getAmount().doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate.set(Date.from(barEntry.getBeginTime().toInstant()));
			}
			fs.get().setPeriod(getPeriod(series));
			fs.get().setLatestTrade(latestTradeDate.get());
			totalProfit.set(new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue());
			if (!Double.isNaN(totalProfit.get())) {
				fs.get().setTotalProfit(new BigDecimal(totalProfit.get()).setScale(4, BigDecimal.ROUND_DOWN));
			} else {
				fs.get().setTotalProfit(BigDecimal.ZERO);
			}
			//totalGrossReturn = new ReturnCriterion().calculate(series, tradingRecord).doubleValue();
			totalGrossReturn.set(new ProfitCriterion().calculate(series, tradingRecord).doubleValue());
			if (!Double.isNaN(totalGrossReturn.get())) {
				fs.get().setTotalGrossReturn(new BigDecimal(totalGrossReturn.get()).setScale(4, BigDecimal.ROUND_DOWN));
			} else {
				fs.get().setTotalGrossReturn(BigDecimal.ZERO);
			}
			fs.get().setNumberofTrades(new BigDecimal(new NumberOfPositionsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double profitableTradesRatio =  PositionsRatioCriterion.WinningPositionsRatioCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(profitableTradesRatio)) {
				fs.get().setProfitableTradesRatio(new BigDecimal(profitableTradesRatio * 100).setScale(2, RoundingMode.DOWN));
			}
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(maximumDrawdownCriterion)) {
				fs.get().setMaxDD(new BigDecimal(maximumDrawdownCriterion * 100).setScale(2, RoundingMode.DOWN));
			} else {
				fs.get().setMaxDD(BigDecimal.ZERO);
			}
			fs.get().setOpen(tradingRecord.getCurrentPosition().isOpened());
			fs.get().setSqn(new BigDecimal(new SqnCriterion().calculate(series, tradingRecord).doubleValue()));
			fs.get().setExpectency(new BigDecimal(new ExpectancyCriterion().calculate(series, tradingRecord).doubleValue()));
			AtomicReference<FeaturedStrategy> fsRes = new AtomicReference<>(featuredStrategyRepository.save(fs.get()));
			//StrategyTrade
			List<StrategyTrade>  existingStrategyTrades = tradesRepository.findByFeaturedStrategyId(fsRes.get().getId());

			if (!existingStrategyTrades.isEmpty()) {
				tradesRepository.deleteAllInBatch(existingStrategyTrades);
			}
			strategyTradeList.forEach(st -> {
				st.setFeaturedStrategy(fsRes.get());
				tradesRepository.save(st);
			});
			//StrategyIndicatorValue
			List<StrategyIndicatorValue>  existIv = indicatorValueRepo.findByFeaturedStrategyId(fsRes.get().getId());
			if (!existIv.isEmpty()) {
				indicatorValueRepo.deleteAllInBatch(existIv);
			}
			strategiesMap.getIndicatorValues(strategy, null).forEach(iv-> {
				iv.setFeaturedStrategy(fsRes.get());
				indicatorValueRepo.save(iv);
			});
		});
	}

	protected void setBestStrategy(BarSeries barSeries) {
		if (Objects.isNull(barSeries) || barSeries.getBarData().isEmpty()){
			return;
		}
		List<StrategyPerformance>  existSp = strategyPerformanceRepository.findBySecurityNameAndDate(barSeries.getName(),DateTimeManager.get(barSeries.getLastBar().getEndTime()));
		if (!existSp.isEmpty()) {
			existSp.forEach(sp -> {
				strategyPerformanceRepository.delete(sp);
			});
		}
		if (barSeriesService.getAmount(barSeries) != null) {
			List<Strategy> strategies = strategiesMap.getStrategies(barSeries);
			AnalysisCriterion profitCriterion = new ReturnCriterion();
			BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);
			BacktestExecutor backtestExecutor = new BacktestExecutor(barSeries);
			List<TradingStatement> tradingStatements;
			Num amount = null;
			try {
				amount = barSeriesService.getAmount(barSeries);
				tradingStatements = backtestExecutor.execute(strategies, amount);
			} catch (Exception e) {
				throw new RuntimeException("amount "+ amount+ ", barSeries.getName " +barSeries.getName(), e);
			}
			Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
			StrategyPerformance strategyPerformance = new StrategyPerformance();
			strategyPerformance.setDate(DateTimeManager.get(barSeries.getLastBar().getEndTime()));
			strategyPerformance.setBestStrategy(bestStrategy.getName());
			strategyPerformance.setTotalProfitLoss(getTotalPnLPercentage(tradingStatements, bestStrategy));
			strategyPerformance.setSecurityName(barSeries.getName());
			try {
				strategyPerformanceRepository.saveAndFlush(strategyPerformance);
			} catch (Exception e) {
				log.error("\nStrategyPerformance entity:"+ ReflectionToStringBuilder.toString(strategyPerformance));
				throw new RuntimeException(e);
			}
		}
	}

	private static BigDecimal getTotalPnLPercentage(List<TradingStatement> tradingStatements, Strategy bestStrategy) {
		Optional<TradingStatement> bestTradingStatement = tradingStatements.stream()
				.filter(s -> s.getStrategy().getName().equals(bestStrategy.getName())).findFirst();
		double pnl = bestTradingStatement.get().getPerformanceReport().getTotalProfitLossPercentage().doubleValue();
		return new BigDecimal(pnl).setScale(2, BigDecimal.ROUND_DOWN);
	}

	private static BigDecimal getTotalPnL(List<TradingStatement> tradingStatements, Strategy bestStrategy) {
		Optional<TradingStatement> bestTradingStatement = tradingStatements.stream()
				.filter(s -> s.getStrategy().getName().equals(bestStrategy.getName())).findFirst();
		double pnl = bestTradingStatement.get().getPerformanceReport().getTotalProfitLoss().doubleValue();
		return new BigDecimal(pnl).setScale(2, BigDecimal.ROUND_DOWN);
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
