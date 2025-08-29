package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.Profit;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.hedge.BetaStrategy;
import nu.itark.frosk.strategies.hedge.GoldStrategy;
import nu.itark.frosk.strategies.hedge.VIXStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BacktestExecutor;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.*;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.PerformanceReport;
import org.ta4j.core.reports.PerformanceReportGenerator;
import org.ta4j.core.reports.TradingStatement;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * Oskar?: 734 818 634-2
 * Rasmus: 8368-3,694 961 403-3
 *
 *
 */



@SpringBootTest
@Slf4j
public class TestJStrategies extends BaseIntegrationTest {

	@Autowired
	BarSeriesService barSeriesService;

	@Autowired
	StrategyAnalysis strategyAnalysis;

	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;

	@Autowired
	StrategyTradeRepository strategyTradeRepository;

	@Autowired
	StrategiesMap strategiesMap;

	Formatter fmt;


	@BeforeEach
	public void addFormat() {
		fmt = new Formatter();
		fmt.format("%-25s %-15s %-15s %-15s %-15s %-15s %-15s %-20s %-30s\n","Strategy",
													"Security",
													"Begin",
													"End",
													"Open",
													"Total profit",
													"Number of bars",
													"Number of trades",
													"Profitable trades ratio");
	}

	@Test
	public void runAllSingleDataSet() {
		List<ReturnObject> resultMap = new ArrayList<>();
		String productId = "FRAG.ST"; //"FRAG.ST", "ABB.ST"
		log.info("runAllSingleDataSet, productId:{}", productId);
		addFormat();
		BarSeries timeSeries = barSeriesService.getDataSet(productId, false, false);

		HighLanderStrategy hl = strategiesMap.getHighLanderStrategy();
		resultMap.add(run(hl.buildStrategy(timeSeries),timeSeries));


/*
		BetaStrategy beta = strategiesMap.getBetaStrategy();
		resultMap.add(run(beta.buildStrategy(timeSeries),timeSeries));
*/

/*
		GoldStrategy gold = strategiesMap.getGoldStrategy();
		resultMap.add(run(gold.buildStrategy(),timeSeries));
*/


/*
		VIXStrategy vix = strategiesMap.getVixStrategy();
		resultMap.add(run(vix.buildStrategy(),timeSeries));
*/



/*		HedgeIndexStrategy hedge = strategiesMap.getHedgeIndexStrategy();
		resultMap.add(run(hedge.buildStrategy(timeSeries),timeSeries));

*/


//		VWAPStrategy vwap = new VWAPStrategy(timeSeries);
//		run(vwap.buildStrategy(),timeSeries);

/*
		RSI2Strategy rsi = strategiesMap.getRsiStrategy();
		rsi.inherentExitRule = true;
		resultMap.add(run(rsi.buildStrategy(timeSeries),timeSeries));
*/

//		MovingMomentumStrategy mm =  new MovingMomentumStrategy(timeSeries);
//		run(mm.buildStrategy(),timeSeries);

//		SimpleMovingMomentumStrategy smm =  new SimpleMovingMomentumStrategy(timeSeries);
//		run(smm.buildStrategy(),timeSeries);

//		GlobalExtremaStrategy ge1 = new GlobalExtremaStrategy(timeSeries);
//		run(ge1.buildStrategy(),timeSeries);
//		
//		GlobalExtremaStrategy ge2 = new GlobalExtremaStrategy(timeSeries);
//		run(ge2.buildStrategy(2),timeSeries);

//		GlobalExtremaStrategy ge3 = new GlobalExtremaStrategy(timeSeries);
//		run(ge3.buildStrategy(2,2),timeSeries);
		
//		CCICorrectionStrategy cci = new CCICorrectionStrategy(timeSeries);
//		run(cci.buildStrategy(),timeSeries);
		

/*
		EngulfingStrategy eng = strategiesMap.getEngulfingStrategy();
		resultMap.add(run(eng.buildStrategy(timeSeries),timeSeries));
*/


/*
		HaramiStrategy harami = strategiesMap.getHaramiStrategy();
		resultMap.add(run(harami.buildStrategy(timeSeries),timeSeries));
		harami.inherentExitRule = false;
		resultMap.add(run(harami.buildStrategy(timeSeries),timeSeries));
*/

/*
		ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(timeSeries);
		resultMap.add(run(three.buildStrategy(),timeSeries));

		ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(timeSeries);
		resultMap.add(run(cd.buildStrategy(),timeSeries));

*/
		/*
		SimpleMovingMomentumStrategy simpleMa = new SimpleMovingMomentumStrategy(timeSeries);
		resultMap.add(run(simpleMa.buildStrategy(),timeSeries));

		 */

/*


		Strategy strategy = new ADXStrategy(timeSeries).buildStrategy();
		resultMap.add(run(strategy,timeSeries));
*/


/*
		EMATenTenStrategy emaTT = strategiesMap.getEmaTenTenStrategy();
		resultMap.add(run(emaTT.buildStrategy(timeSeries),timeSeries));
*/


		printResult(resultMap);

	}

	@Test
	public void runAllDataSetList() {
		List<ReturnObject> resultMap = new ArrayList<>();
		List<BarSeries> timeSeriesList = barSeriesService.getDataSet(Database.YAHOO);
		timeSeriesList.forEach(ts -> {
//			RSI2Strategy rsi = new RSI2Strategy(ts);
//			resultMap.add(run(rsi.buildStrategy(), ts));

/*
			ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(ts);
			resultMap.add(run(three.buildStrategy(), ts));
*/

//			EngulfingStrategy eng = new EngulfingStrategy(ts);
//			resultMap.add(run(eng.buildStrategy(), ts));

/*
			ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(ts);
			resultMap.add(run(cd.buildStrategy(), ts));
*/

/*
			SimpleMovingMomentumStrategy simpleMa = new SimpleMovingMomentumStrategy(ts);
			resultMap.add(run(simpleMa.buildStrategy(), ts));
*/


		//	ADXStrategy adx = new ADXStrategy();
/*
			try {
				resultMap.add(run(strategiesMap.getHedgeIndexStrategy().buildStrategy(ts), ts));
			} catch (Exception e) {
				System.out.println("getHedgeIndexStrategy().buildStrategy(ts), Name:"+ts.getName());
				throw new RuntimeException(e);
			}
*/

			try {
				if (ts.isEmpty()) {
					//log.info("ts:{} is empty",ts.getName());
					return;
				}
				resultMap.add(run(strategiesMap.getHighLanderStrategy().buildStrategy(ts), ts));
			} catch (Exception e) {
				System.out.println("getHighLanderStrategy().buildStrategy(ts), Name:"+ts.getName());
				throw new RuntimeException(e);
			}

		});
		printResult(resultMap);


	}

	@Test
	public void runOneSingleDataSet2() {
	log.info("runOneSingleDataSet2");
	String securityName = "FRAG.ST";

	BarSeries series = barSeriesService.getDataSet(securityName, false, false);
	for (int i = 0; i < series.getBarCount(); i++) {
		Bar bar = series.getBar(i);
		log.info("series.ClosePrice:{}, EndTime:{}", bar.getClosePrice().toString(), bar.getEndTime());
	}

	BarSeries adjustedSeries = StockSplitAdjustedBarSeries.adjust(series);
	for (int i = 0; i < adjustedSeries.getBarCount(); i++) {
		Bar bar = adjustedSeries.getBar(i);
		log.info("adjustedSeries.ClosePrice:{}, EndTime:{}", bar.getClosePrice().toString(), bar.getEndTime());
	}

	Strategy strategy = strategiesMap.getHighLanderStrategy().buildStrategy(adjustedSeries);
	// Strategy strategy = strategiesMap.getHighLanderStrategy().buildStrategy(series);
	//Strategy strategy = strategiesMap.getEngulfingStrategy().buildStrategy(series);
	//Strategy strategy = new SimpleMovingMomentumStrategy().buildStrategy(series);
	//Strategy strategy = new ADXStrategy(series).buildStrategy();

	TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategy);
	for (Position position : tradingRecord.getPositions()) {
		Bar barEntry = series.getBar(position.getEntry().getIndex());
		System.out.println(series.getName()+"::barEntry="+barEntry.getEndTime());
		System.out.println(series.getName()+"::barEntry.getClosePrice="+ barEntry.getClosePrice());
		Bar barExit = series.getBar(position.getExit().getIndex());
		System.out.println(series.getName()+"::barExit="+barExit.getDateName());
		System.out.println(series.getName()+"::barExit.getClosePrice="+ barExit.getClosePrice());
		System.out.println(series.getName()+"::getNetPrice="+ position.getEntry().getNetPrice());

		System.out.println("profit(position): " + position.getProfit());
		System.out.println("Gross return(position, percentage): " + position.getGrossReturn());
		System.out.println("Gross profit(position): " + position.getGrossProfit());
/*
		Num pnl = barExit.getClosePrice().dividedBy(barEntry.getClosePrice()).multipliedBy(series.numOf(100));
		System.out.println("P/L: " + pnl.doubleValue());
*/
	}


	// Total return Xtra
	ProfitCriterion totalprofit = new ProfitCriterion();
        System.out.println("\nTotal profit(pengar): " + totalprofit.calculate(series, tradingRecord).doubleValue());
	// Total profit
	ReturnCriterion totalReturn = new ReturnCriterion();
        System.out.println("Total return: " + totalReturn.calculate(series, tradingRecord).doubleValue());

	ProfitLossPercentageCriterion totalPercentage = new ProfitLossPercentageCriterion();
		System.out.println("Total pnl percentage: " + totalPercentage.calculate(series, tradingRecord).doubleValue());
	// Number of bars
//        System.out.println("Number of bars: " + new NumberOfBarsCriterion().calculate(series, tradingRecord));
	// Average profit (per bar)
        System.out.println(
				"Average return (per bar): " + new AverageReturnPerBarCriterion().calculate(series, tradingRecord));
	// Number of positions
        System.out.println("Number of positions: " + new NumberOfPositionsCriterion().calculate(series, tradingRecord));
	// Profitable position ratio
        System.out.println(
				"Winning positions ratio: " + PositionsRatioCriterion.WinningPositionsRatioCriterion().calculate(series, tradingRecord));
	// Maximum drawdown
        System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
	// Reward-risk ratio
        System.out.println("Return over maximum drawdown: "
				+ new ReturnOverMaxDrawdownCriterion().calculate(series, tradingRecord));
	// Total transaction cost
        System.out.println("Total transaction cost (from $1000): "
				+ new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
	// Total profit vs buy-and-hold
        System.out.println("Custom strategy return vs enter-and-hold strategy return: "
				+ new VersusEnterAndHoldCriterion(totalReturn).calculate(series, tradingRecord));

		doPerformanceReport(series, strategy, tradingRecord);

	}

	private static void doPerformanceReport(BarSeries series, Strategy strategy, TradingRecord tradingRecord) {
		PerformanceReportGenerator performanceReportGenerator = new  PerformanceReportGenerator();
		final PerformanceReport performanceReport = performanceReportGenerator.generate(strategy, tradingRecord, series);
		System.out.println("performanceReport="+ ReflectionToStringBuilder.toString(performanceReport));
	}


	private void printResult(List<ReturnObject> resultMap) {
		List<ReturnObject> resultMapOrdered = resultMap.stream()
				.sorted(Comparator.nullsLast(Comparator.comparing(ReturnObject::getSecurityName)))
				.collect(Collectors.toList());
		addFormat();
		List<Num> totalProfitAcc= new ArrayList<>();
		resultMapOrdered.forEach(ro -> {
			BarSeries barSeries;
			TradingRecord tradingRecord;
			Strategy strategy;
			if (Objects.nonNull(ro)) {
				barSeries = ro.seriesManager.getBarSeries();
				tradingRecord = ro.tradingRecord;
				strategy = ro.strategy;
				Num grossReturn = (new ReturnCriterion().calculate(barSeries, tradingRecord));
				Num profitLossPercentage = (new ProfitLossPercentageCriterion().calculate(barSeries, tradingRecord));
				if (!grossReturn.isNaN()) {
					totalProfitAcc.add(grossReturn);
				}
				fmt.format("%-25s %-15s %-15s %-15s %-15s %-15s %-15s %-20s %-30s", strategy.getName().replace("Strategy", "")
						,barSeries.getName(),
						barSeries.getFirstBar().getBeginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
						barSeries.getLastBar().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
						tradingRecord.getCurrentPosition().isOpened() ? getDateByIndex(barSeries, tradingRecord.getCurrentPosition().getEntry().getIndex()) : "",
						grossReturn,
						new NumberOfBarsCriterion().calculate(barSeries, tradingRecord),
						new NumberOfPositionsCriterion().calculate(barSeries, tradingRecord),
						percent(PositionsRatioCriterion.WinningPositionsRatioCriterion().calculate(barSeries, tradingRecord).doubleValue()));
				System.out.println(fmt);
				fmt = new Formatter();
			}
		});

		Double averageProfit = totalProfitAcc.stream()
				.map(p -> p.doubleValue())
				.collect(Collectors.averagingDouble(Double::doubleValue));
		System.out.println("Average on all TotalProfit(eget):"+averageProfit);
	}

	ReturnObject run(Strategy strategy, BarSeries series) {
		BarSeriesManager seriesManager = new BarSeriesManager(series);
		TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategy);
		List<Position> trades = tradingRecord.getPositions();
		ReturnObject returnObject = new ReturnObject(strategy,seriesManager, tradingRecord);

		if (trades.isEmpty()) {
			return null;
		}

		//doPerformanceReport(timeSeries, strategy, tradingRecord);

		return returnObject;
	}

	@Test
	public void chooseBestForSecurity() {
		BarSeries timeSeries = barSeriesService.getDataSet("BAT-EUR", false, false);
		List<Strategy> strategies = strategiesMap.getStrategies(timeSeries);

		chooseBestForSecurity(new ProfitCriterion(), timeSeries, strategies);
		chooseBestForSecurity(new ProfitLossPercentageCriterion(), timeSeries, strategies);
		chooseBestForSecurity(new SqnCriterion(), timeSeries, strategies);
		chooseBestForSecurity(new ExpectancyCriterion(), timeSeries, strategies);
/*
		BarSeriesManager timeSeriesManager = new BarSeriesManager(timeSeries, costs.getTransactionCostModel(), costs.getBorrowingCostModel());
		for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
			Strategy strategy = entry.getKey();
			String name = entry.getPrice();
			TradingRecord tradingRecord = timeSeriesManager.run(strategy);
			double profit = profitCriterion.calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tGross return(%) for " + name + ": " + percent(profit));
			double pnl = new ProfitLossPercentageCriterion().calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tProfitLoss for " + name + ": " + pnl);
		}
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
		System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");
*/
	}


	private Strategy chooseBestForSecurity(AnalysisCriterion criterion,BarSeries timeSeries, List<Strategy> strategies) {
		BarSeriesManager timeSeriesManager = new BarSeriesManager(timeSeries);
/*
		for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
			Strategy strategy = entry.getKey();
			String name = entry.getPrice();
			TradingRecord tradingRecord = timeSeriesManager.run(strategy);
			double profit = criterion.calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tcriterion "+ criterion.getClass().getName() + ", for " + name + ": " + profit);
		}
*/
		Strategy bestStrategy = criterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
		System.out.println("\t\t--> Best strategy: " + bestStrategy + "\n");
		return bestStrategy;
	}

	@Test
	public void chooseBestForSecurity2() {
		BarSeries barSeries = barSeriesService.getDataSet("BTC-EUR", false, false);
		List<Strategy> strategies = strategiesMap.getStrategies(barSeries);
		AnalysisCriterion profitCriterion = new ReturnCriterion();
		BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);
		BacktestExecutor backtestExecutor = new BacktestExecutor(barSeries);
		final List<TradingStatement> tradingStatements = backtestExecutor.execute(strategies, DoubleNum.valueOf(50), Trade.TradeType.BUY);
		for (TradingStatement tradingStatement : tradingStatements) {
			System.out.println(tradingStatement.getStrategy().getName() +":"+ tradingStatement.getPerformanceReport().getTotalProfitLossPercentage().getDelegate() + "%");
		}
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
		System.out.println("\t\t--> Best strategy: " + bestStrategy.getName() + "\n");
		Optional<TradingStatement> bestTradingStatement = tradingStatements.stream()
				.filter(s -> s.getStrategy().getName().equals(bestStrategy.getName())).findFirst();
		if (bestTradingStatement.isPresent()) {
			System.out.println("best:"+bestTradingStatement.get().getPerformanceReport().getTotalProfitLossPercentage());
		} else {
			System.err.println("no match:"+bestStrategy);
		}

	}

	@Test
	public void chooseBestForAllSecurities() {
		double highestProfitProfit = 0;
		String highestProfitNameProfit = "";
		String highestProfitNameStrategyProfit = "";

		double highestProfitNrTrade = 0;
		String highestProfitNameNrTrade = "";
		String highestProfitNameStrategyNrTrade = "";

		List<BarSeries> timeSeriesList = barSeriesService.getDataSet(Database.COINBASE);
		// The analysis criterion
		AnalysisCriterion profitCriterion = new ReturnCriterion();
		// The analysis criterion
		AnalysisCriterion nrOfTradesCriterion = new NumberOfPositionsCriterion();
		
		for (BarSeries timeSeries : timeSeriesList) {
			List<Strategy> strategies = strategiesMap.getStrategies(timeSeries);
			BarSeriesManager timeSeriesManager = new BarSeriesManager(timeSeries);
/*
			for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
				Strategy strategy = entry.getKey();
				String name = entry.getPrice();
				TradingRecord tradingRecord = timeSeriesManager.run(strategy);
				double profitProfit = profitCriterion.calculate(timeSeries, tradingRecord).doubleValue();
				if (profitProfit > highestProfitProfit) {
					highestProfitProfit = profitProfit;
					highestProfitNameProfit = timeSeries.getName();
					highestProfitNameStrategyProfit = strategy.getName();
				}
				double profitNrTrade = nrOfTradesCriterion.calculate(timeSeries, tradingRecord).doubleValue();
				if (profitNrTrade > highestProfitNrTrade) {
					highestProfitNrTrade = profitNrTrade;
					highestProfitNameNrTrade = timeSeries.getName();
					highestProfitNameStrategyNrTrade = strategy.getName();
				}
			}
*/
			Strategy bestProfitStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
			System.out.println("\t\t--> Best strategy(profit criteria) for : "+timeSeries.getName()+" = " + bestProfitStrategy);
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameProfit + " profit%: " + percent(highestProfitProfit));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategyProfit + "\n");
			Strategy bestNrOfTradesStrategy = nrOfTradesCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
/*
			System.out.println("\t\t--> Best strategy(nr of trades) for : "+timeSeries.getName()+" = " + strategies.get(bestNrOfTradesStrategy));
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameNrTrade + " profit%: " + percent(highestProfitNrTrade));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategyNrTrade + "\n");
*/
		}

	}	
	
	private String percent(double value) {
		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		format.setMinimumFractionDigits(1);
		double raw;
		if (value > 1) {
			raw = value -1;
		} else {
			raw = value;
		}
		return format.format(raw);
	}


	private String percent(Num n) {
		return !n.isNaN() ? percent(n.doubleValue()) : "N_a_N";
	}

	private String getDateByIndex(BarSeries barSeries, int idx) {
		return barSeries.getBar(idx).getBeginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}

	class ReturnObject {
		Strategy strategy;
		BarSeriesManager seriesManager;
		TradingRecord tradingRecord;
		String securityName = null;

		public ReturnObject(Strategy strategy, BarSeriesManager seriesManager, TradingRecord tradingRecord){
			this.strategy = strategy;
			this.seriesManager = seriesManager;
			this.tradingRecord = tradingRecord;
			this.securityName = seriesManager.getBarSeries().getName();
		};

		public String getSecurityName() {
			return securityName;
		}
	}

	@Test
	public void testMostAppropriateWayOfPerformanceMeasurement() {
	/*
		Measurements:
		- Init amount ?
		- GrossReturn -> In percent
		- Lite nytt: SqnCriterion och ExpectancyCriterion, kanske combo
		- TotalCurrency : SEK ?

	*/
		String securityName = "FIL-EUR";
		BarSeries series = barSeriesService.getDataSet(securityName, false, false);
		Strategy strategy = new HaramiStrategy().buildStrategy(series);


		BarSeriesManager seriesManager = new BarSeriesManager(series);
		TradingRecord tradingRecord = seriesManager.run(strategy);
		//1 . StrategyAnalysis
		System.out.println("********************strategyAnalysis.run***********************");
		Long sec_id = barSeriesService.getSecurityId(securityName);
		strategyAnalysis.run(strategy.getName(), sec_id);
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy.getName(), series.getName());
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				.peek(t-> System.out.println("date="+t.getDate() + ",type="+t.getType() + ",price="+t.getPrice() + ",grossProfit="+t.getGrossProfit() + ",pnl="+t.getPnl()))
				.collect(Collectors.toSet());
		Double averageProfit = fs.getStrategyTrades().stream()
				.filter(p-> Objects.nonNull(p.getPnl()))
				.map(p -> p.getPnl().doubleValue())
				.collect(Collectors.averagingDouble(Double::doubleValue));
		System.out.println("Average on all PnL(eget):"+averageProfit);
		//2. printResult(resultMap)
		System.out.println("********************printResult(resultMap)***********************");
		List<ReturnObject> resultMap = new ArrayList<>();
		resultMap.add(run(strategy,series));
		printResult(resultMap);
		//3. doPerformanceReport
		System.out.println("********************performanceReportGenerator***********************");
		PerformanceReportGenerator performanceReportGenerator = new  PerformanceReportGenerator();
		PerformanceReport performanceReport = performanceReportGenerator.generate(strategy, tradingRecord, series);
		System.out.println("performanceReport="+ ReflectionToStringBuilder.toString(performanceReport));
		//4. GrossReturn and ProfitLossPercentage
		System.out.println("********************Criterion***********************");
		LossCriterion lossCriterion = new LossCriterion();
		ReturnCriterion returnCriterion = new ReturnCriterion();
		ProfitLossPercentageCriterion profitLossPercentageCriterion = new ProfitLossPercentageCriterion();
		ProfitLossCriterion profitLossCriterion = new ProfitLossCriterion();
		System.out.println("lossCriterion:"+lossCriterion.calculate(series, tradingRecord));
		System.out.println("returnCriterion:"+returnCriterion.calculate(series, tradingRecord));
		System.out.println("profitLossCriterion:"+profitLossCriterion.calculate(series, tradingRecord));
		System.out.println("profitLossPercentageCriterion:"+ profitLossPercentageCriterion.calculate(series, tradingRecord));

		//6. ProfitLossCriterion
		System.out.println("********************Manuell ProfitLossCriterion***********************");
		Object profitLoss = tradingRecord.getPositions().stream().filter(Position::isClosed)
				.map(position -> calculate(series, position)).reduce(series.numOf(0), Num::plus);
		System.out.println("profitLoss:"+ profitLoss);

		//.7 ,lite nytt
		System.out.println("********************Q-Criterion***********************");
		AnalysisCriterion sqnCriterion = new SqnCriterion();
		System.out.println("sqnCriterion:"+sqnCriterion.calculate(series, tradingRecord));
		AnalysisCriterion expCriterion = new ExpectancyCriterion();
		System.out.println("expCriterion:"+expCriterion.calculate(series, tradingRecord));
	}

	public Num calculate(BarSeries series, Position position) {
		return position.getProfit();
	}

	@Test
	public void runADX() {
		Long sec_id = barSeriesService.getSecurityId("BTC-EUR");
		strategyAnalysis.run(ADXStrategy.class.getSimpleName(), sec_id);
		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(ADXStrategy.class.getSimpleName(), "BTC-EUR");
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t)))
				.collect(Collectors.toSet());

		final Profit totalGrossProfitForStrategy = strategyTradeRepository.findTotalGrossProfitForStrategy(fs.getId());
		System.out.println("totalGrossProfitForStrategy:"+totalGrossProfitForStrategy.getGrossProfit() + "fsId: "+fs.getId());
	}

}
