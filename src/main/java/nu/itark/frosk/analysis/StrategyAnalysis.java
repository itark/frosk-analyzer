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
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BacktestExecutor;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.TradingStatement;

import java.math.BigDecimal;
import java.util.*;


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
	StrategyExecutor strategyExecutor;

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

	public void run(String strategy, Long security_id) throws DataIntegrityViolationException {
		log.info("run({},{})", strategy, security_id);
		if (Objects.isNull(strategy) && Objects.isNull(security_id)) {
			// Case 1: both null - run all strategies on all securities
			List<String> strategies = strategiesMap.buildStrategiesMap();
			strategies.removeAll(List.of(excludeHedgeStrategies));
			if (strategyOnly != null && !strategyOnly.isEmpty()) {
				strategies = List.of(strategyOnly);
			}
			log.info("{} strategies to run.", strategies.size());
			hedgeIndexService.warmCache();
			// Load bar series ONCE and reuse across all strategies
			List<BarSeries> barSeriesList = barSeriesService.getDataSet(Database.valueOf(databaseOnly));
			log.info("Loaded {} bar series.", barSeriesList.size());
			runStrategiesInParallel(strategies, barSeriesList);
		}
		else if (Objects.isNull(strategy) && Objects.nonNull(security_id)) {
			// Case 2: strategy null, security_id set - run all strategies on one security
			List<String> strategies = strategiesMap.buildStrategiesMap();
			strategies.removeAll(List.of(excludeHedgeStrategies));
			if (strategyOnly != null && !strategyOnly.isEmpty()) {
				strategies = List.of(strategyOnly);
			}
			List<BarSeries> barSeriesList = List.of(barSeriesService.getDataSet(security_id));
			runStrategiesInParallel(strategies, barSeriesList);
		}
		else if (Objects.nonNull(strategy) && Objects.isNull(security_id)) {
			// Case 3: strategy set, security_id null - run one strategy on all securities
			try {
				strategyExecutor.execute(strategy, barSeriesService.getDataSet(Database.valueOf(databaseOnly)));
			} catch (Exception e) {
				log.error("Error runStrategy on strategy={}", strategy);
				throw e;
			}
		}
		else if (Objects.nonNull(strategy) && security_id != null) {
			// Case 4: both set - run one strategy on one security
			BarSeries barSeries = barSeriesService.getDataSet(security_id);
			if (Objects.isNull(barSeries) || barSeries.isEmpty()) {
				throw new RuntimeException("BarSeries is null or empty. Download security prices.");
			}
			try {
				strategyExecutor.execute(strategy, List.of(barSeries));
			} catch (Exception e) {
				log.error("Error runStrategy on strategy={} and security_id={}", strategy, security_id);
				throw e;
			}
		}
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		log.info("run({},{}) READY", strategy, security_id);
	}

	private void runStrategiesInParallel(List<String> strategies, List<BarSeries> barSeriesList) {
		strategies.forEach(strategyName -> {
			try {
				strategyExecutor.execute(strategyName, barSeriesList);
			} catch (DataIntegrityViolationException e) {
				log.error("Error executing strategyName={}", strategyName, e);
				throw e;
			}
		});
	}

	// ...existing code...

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

}
