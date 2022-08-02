package nu.itark.frosk.strategies;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.*;
import org.ta4j.core.analysis.criteria.pnl.GrossProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.analysis.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.TradingStatement;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategies {

	Logger logger = Logger.getLogger(TestJStrategies.class.getName());

	@Autowired
	BarSeriesService barSeriesService;

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
		String productId = "BTC-EUR";  //BTC-EUR,BTC-USDT, BCH-EUR, AAVE-EUR, ETC-EUR, WLUNA-EUR, BTRST-EUR
		addFormat();
		BarSeries timeSeries = barSeriesService.getDataSet(productId);

//		VWAPStrategy vwap = new VWAPStrategy(timeSeries);
//		run(vwap.buildStrategy(),timeSeries);

//		RSI2Strategy rsi = new RSI2Strategy(timeSeries);
//		run(rsi.buildStrategy(),timeSeries);
		
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
		EngulfingStrategy eng = new EngulfingStrategy(timeSeries);
		run(eng.buildStrategy(),timeSeries);
*/

//		HaramiStrategy harami = new HaramiStrategy(timeSeries);
//		run(harami.buildStrategy(),timeSeries);	
	
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

		ADXStrategy adx = new ADXStrategy();
		resultMap.add(run(adx.buildStrategy(timeSeries),timeSeries));


		printResult(resultMap);

	}

	
	@Test
	public void runAllDataSetList() {
		List<ReturnObject> resultMap = new ArrayList<>();
		List<BarSeries> timeSeriesList = barSeriesService.getDataSet();
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

			ADXStrategy adx = new ADXStrategy();
			resultMap.add(run(adx.buildStrategy(ts), ts));



		});

		printResult(resultMap);

	}

	@Test
	public void runOneSingleDataSet2() {
	BarSeries series = barSeriesService.getDataSet("BTC-EUR");
	Strategy strategy = new SimpleMovingMomentumStrategy(series).buildStrategy();
	BarSeriesManager seriesManager = new BarSeriesManager(series);
	TradingRecord tradingRecord = seriesManager.run(strategy);

	for (Position position : tradingRecord.getPositions()) {
		Bar barEntry = series.getBar(position.getEntry().getIndex());
		System.out.println(series.getName()+"::barEntry="+barEntry.getEndTime());
		System.out.println(series.getName()+"::barEntry.getClosePrice="+ barEntry.getClosePrice());
		Bar barExit = series.getBar(position.getExit().getIndex());
		System.out.println(series.getName()+"::barExit="+barExit.getDateName());
		System.out.println(series.getName()+"::barExit.getClosePrice="+ barExit.getClosePrice());
		double delta = barExit.getClosePrice().minus(barEntry.getClosePrice()).doubleValue();
		System.out.println("delta(position): " + delta);
		System.out.println("Gross return(position): " + position.getGrossReturn());
		System.out.println("Gross profit(position): " + position.getGrossProfit());
	}

	// Total return Xtra
	GrossProfitCriterion totalprofit = new GrossProfitCriterion();
        System.out.println("Total profit: " + percent(totalprofit.calculate(series, tradingRecord).doubleValue()));
	// Total profit
	GrossReturnCriterion totalReturn = new GrossReturnCriterion();
        System.out.println("Total return: " + percent(totalReturn.calculate(series, tradingRecord).doubleValue()));
	// Number of bars
        System.out.println("Number of bars: " + new NumberOfBarsCriterion().calculate(series, tradingRecord));
	// Average profit (per bar)
        System.out.println(
				"Average return (per bar): " + new AverageReturnPerBarCriterion().calculate(series, tradingRecord));
	// Number of positions
        System.out.println("Number of positions: " + new NumberOfPositionsCriterion().calculate(series, tradingRecord));
	// Profitable position ratio
        System.out.println(
				"Winning positions ratio: " + new WinningPositionsRatioCriterion().calculate(series, tradingRecord));
	// Maximum drawdown
        System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
	// Reward-risk ratio
        System.out.println("Return over maximum drawdown: "
				+ new ReturnOverMaxDrawdownCriterion().calculate(series, tradingRecord));
	// Total transaction cost
        System.out.println("Total transaction cost (from $1000): "
				+ new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
	// Buy-and-hold
        System.out.println("Buy-and-hold return: " + new BuyAndHoldReturnCriterion().calculate(series, tradingRecord));
	// Total profit vs buy-and-hold
        System.out.println("Custom strategy return vs buy-and-hold strategy return: "
				+ new VersusBuyAndHoldCriterion(totalReturn).calculate(series, tradingRecord));
}




	private void printResult(List<ReturnObject> resultMap) {
		addFormat();
		List<Num> totalProfitAcc= new ArrayList<>();
		resultMap.forEach(ro -> {
			BarSeries barSeries;
			TradingRecord tradingRecord;
			Strategy strategy;
			if (Objects.nonNull(ro)) {
				barSeries = ro.seriesManager.getBarSeries();
				tradingRecord = ro.tradingRecord;
				strategy = ro.strategy;
				Num totalProfit = (new GrossReturnCriterion().calculate(barSeries, tradingRecord));
				totalProfitAcc.add(totalProfit);
				fmt.format("%-25s %-15s %-15s %-15s %-15s %-15s %-15s %-20s %-30s", strategy.getName().replace("Strategy", "")
						,barSeries.getName(),
						barSeries.getFirstBar().getBeginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
						barSeries.getLastBar().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
						tradingRecord.getCurrentPosition().isOpened() ? getDateByIndex(barSeries, tradingRecord.getCurrentPosition().getEntry().getIndex()) : "",
						percent(totalProfit.doubleValue()),
						new NumberOfBarsCriterion().calculate(barSeries, tradingRecord),
						new NumberOfPositionsCriterion().calculate(barSeries, tradingRecord),
						percent(new WinningPositionsRatioCriterion().calculate(barSeries, tradingRecord).doubleValue()));
				System.out.println(fmt);
				fmt = new Formatter();
			}
		});

		Double averageProfit = totalProfitAcc.stream()
				.map(p -> p.doubleValue())
				.collect(Collectors.averagingDouble(Double::doubleValue));
		System.out.println("Average on all TotalProfit:"+percent(averageProfit));
	}

	private String getDateByIndex(BarSeries barSeries, int idx) {
		return barSeries.getBar(idx).getBeginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}


	ReturnObject run(Strategy strategy, BarSeries timeSeries) {
		//logger.info("****** "+strategy.getName()+ ", "+timeSeries.getName()+" *******");
		BarSeriesManager seriesManager = new BarSeriesManager(timeSeries);
		TradingRecord tradingRecord = seriesManager.run(strategy);
		List<Position> trades = tradingRecord.getPositions();
		ReturnObject returnObject = new ReturnObject(strategy,seriesManager, tradingRecord);

		if (trades.isEmpty()) {
			return null;
		}

		return returnObject;
	}



	
	@Test
	public void chooseBestForSecurity() {
		BarSeries timeSeries = barSeriesService.getDataSet("BTC-EUR");
		Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(timeSeries);
		// The analysis criterion
		AnalysisCriterion profitCriterion = new GrossReturnCriterion();

		BarSeriesManager timeSeriesManager = new BarSeriesManager(timeSeries);
		for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
			Strategy strategy = entry.getKey();
			String name = entry.getValue();
			TradingRecord tradingRecord = timeSeriesManager.run(strategy);
			double profit = profitCriterion.calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tGross return(%) for " + name + ": " + percent(profit));
			double pnl = new ProfitLossPercentageCriterion().calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tProfitLoss for " + name + ": " + pnl);
		}
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
		System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");
	}

	@Test
	public void chooseBestForSecurity2() {
		BarSeries barSeries = barSeriesService.getDataSet("BTC-EUR");
		//Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(barSeries);
		List<Strategy> strategies = StrategiesMap.getStrategies(barSeries);
		// The analysis criterion
		AnalysisCriterion profitCriterion = new GrossReturnCriterion();
		BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);

		BacktestExecutor backtestExecutor = new BacktestExecutor(barSeries);
		final List<TradingStatement> tradingStatements = backtestExecutor.execute(strategies, DoubleNum.valueOf(50), Trade.TradeType.BUY);

		for (TradingStatement tradingStatement : tradingStatements) {
			System.out.println("******* Strategy: "+tradingStatement.getStrategy().getName()+" ********");
			System.out.println(tradingStatement.getPerformanceReport().getTotalProfitLossPercentage().getDelegate());
		}

	}

	//TODO review
	//@Test
	public void chooseBestForAllSecurities() {
		double highestProfitProfit = 0;
		String highestProfitNameProfit = "";
		String highestProfitNameStrategyProfit = "";

		double highestProfitNrTrade = 0;
		String highestProfitNameNrTrade = "";
		String highestProfitNameStrategyNrTrade = "";

		List<BarSeries> timeSeriesList = barSeriesService.getDataSet();
		// The analysis criterion
		AnalysisCriterion profitCriterion = new GrossReturnCriterion();
		// The analysis criterion
		AnalysisCriterion nrOfTradesCriterion = new NumberOfPositionsCriterion();
		
		for (BarSeries timeSeries : timeSeriesList) {
			Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(timeSeries);
			BarSeriesManager timeSeriesManager = new BarSeriesManager(timeSeries);
			for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
				Strategy strategy = entry.getKey();
				String name = entry.getValue();
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
/*
				System.out.println(
						"\tProfit(Profit) for strategy: " + name + " and security :" + timeSeries.getName() + " = " + profitProfit);
				System.out.println(
						"\tProfit(NrTrade) for strategy: " + name + " and security :" + timeSeries.getName() + " = " + profitNrTrade);
*/
			}
			Strategy bestProfitStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
			System.out.println("\t\t--> Best strategy(profit criteria) for : "+timeSeries.getName()+" = " + strategies.get(bestProfitStrategy));
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameProfit + " profit%: " + percent(highestProfitProfit));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategyProfit + "\n");
			Strategy bestNrOfTradesStrategy = nrOfTradesCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
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

	class ReturnObject {
		Strategy strategy;
		BarSeriesManager seriesManager;
		TradingRecord tradingRecord;

		public ReturnObject(Strategy strategy, BarSeriesManager seriesManager, TradingRecord tradingRecord){
			this.strategy = strategy;
			this.seriesManager = seriesManager;
			this.tradingRecord = tradingRecord;
		};
	}

}
