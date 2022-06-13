package nu.itark.frosk.strategies;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.service.TimeSeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.*;
import org.ta4j.core.num.Num;

import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategies {

	Logger logger = Logger.getLogger(TestJStrategies.class.getName());

	@Autowired
	TimeSeriesService timeSeriesService;

	Formatter fmt = new Formatter();

	@BeforeEach
	public void addFormat() {
		fmt.format("%20s %15s %15s %15s %15s %15s\n","Strategy",
													"Security",
													"Total profit",
													"Number of bars",
													"Number of trades",
													"Profitable trades ratio");
	}

	@Test
	public void runAllSingleDataSet() {
		String productId = "BTRST-EUR";  //BTC-EUR,BTC-USDT, BCH-EUR, AAVE-EUR, ETC-EUR, WLUNA-EUR, BTRST-EUR
//		TimeSeries timeSeries = timeSeriesService.getDataSet("SSAB-B.ST");
//		TimeSeries timeSeries = timeSeriesService.getDataSet("KINV-B.ST");
//		TimeSeries timeSeries = timeSeriesService.getDataSet("BOL.ST");
		//TimeSeries timeSeries = timeSeriesService.getDataSet("MAV.ST");
		//TimeSeries timeSeries = timeSeriesService.getDataSetFromCoinbase(productId);
		TimeSeries timeSeries = timeSeriesService.getDataSet(productId);

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
		
		EngulfingStrategy eng = new EngulfingStrategy(timeSeries);
		run(eng.buildStrategy(),timeSeries);

//		HaramiStrategy harami = new HaramiStrategy(timeSeries);
//		run(harami.buildStrategy(),timeSeries);	
	
//		ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(timeSeries);
//		run(three.buildStrategy(),timeSeries);

//		ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(timeSeries);
//		run(cd.buildStrategy(),timeSeries);


//		SimpleMovingMomentumStrategy simpleMa = new SimpleMovingMomentumStrategy(timeSeries);
// 		run(simpleMa.buildStrategy(),timeSeries);

	}

	
	@Test
	public void runAllDataSetList() {
		List<TimeSeries> timeSeriesList=   timeSeriesService.getDataSet();
		timeSeriesList.forEach(ts -> {
//			RSI2Strategy rsi = new RSI2Strategy(ts);
//			run(rsi.buildStrategy(),ts);
			
//			ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(ts);
//			run(three.buildStrategy(),ts);		
			
//			ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(ts);
//			run(cd.buildStrategy(),ts);

			SimpleMovingMomentumStrategy simpleMa = new SimpleMovingMomentumStrategy(ts);
			run(simpleMa.buildStrategy(),ts);

			
		});

	}	

	void run(Strategy strategy, TimeSeries timeSeries) {
		//logger.info("****** "+strategy.getName()+ ", "+timeSeries.getName()+" *******");
		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
		TradingRecord tradingRecord = seriesManager.run(strategy);
		List<Trade> trades = tradingRecord.getTrades();

		if (trades.isEmpty()) return;

		if (tradingRecord.getCurrentTrade().isOpened()) {
			System.out.println(timeSeries.getName() + " IS OPEN!!!!");
			Bar currentEntry = timeSeries.getBar(tradingRecord.getCurrentTrade().getEntry().getIndex());
			System.out.println(timeSeries.getName() + "::currentEntry=" + currentEntry.getSimpleDateName());
		}

		for (Trade trade : trades) {
			Bar barEntry = timeSeries.getBar(trade.getEntry().getIndex());
			Bar barExit = timeSeries.getBar(trade.getExit().getIndex());
			Num closePriceBuy = barEntry.getClosePrice();
			Num closePriceSell = barExit.getClosePrice();
			Num profit2 = closePriceSell.dividedBy(closePriceBuy);
			//System.out.println("profit2(%)=" + percent(profit2.doubleValue()));
		}

		// Total profit
        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
       // logger.info("********Total profit: " + percent(totalProfit.calculate(timeSeries, tradingRecord).doubleValue())+ "********");
        
        // Number of bars
       // logger.info("Number of bars: " + new NumberOfBarsCriterion().calculate(timeSeries, tradingRecord));
        // Average profit (per bar)
//        logger.info("Average profit (per bar): " + percent(new AverageProfitCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Number of trades
        //logger.info("Number of trades: " + new NumberOfTradesCriterion().calculate(timeSeries, tradingRecord));
        // Profitable trades ratio
        //logger.info("Profitable trades ratio: " + percent(new AverageProfitableTradesCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Maximum drawdown
        //logger.info("Maximum drawdown: " + percent(new MaximumDrawdownCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Reward-risk ratio
        //logger.info("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(timeSeries, tradingRecord));
        // Total transaction cost
        //logger.info("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(timeSeries, tradingRecord));
        // Buy-and-hold
//        logger.info("Buy-and-hold: " + percent(new BuyAndHoldCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Total profit vs buy-and-hold
        //logger.info("Custom strategy profit vs buy-and-hold strategy profit: " + percent(new VersusBuyAndHoldCriterion(totalProfit).calculate(timeSeries, tradingRecord).doubleValue()));


		fmt.format("%20s %14s %14s %14s %14s %17s\n", strategy.getName().replace("Strategy", "")
											,timeSeries.getName(),
											percent(totalProfit.calculate(timeSeries, tradingRecord).doubleValue()),
											new NumberOfBarsCriterion().calculate(timeSeries, tradingRecord),
											new NumberOfTradesCriterion().calculate(timeSeries, tradingRecord),
											percent(new AverageProfitableTradesCriterion().calculate(timeSeries, tradingRecord).doubleValue()));

		System.out.println(fmt);

	}
	
	@Test
	public void chooseBestForSecurity() {
		// Splitting the series into slices
		TimeSeries timeSeries = timeSeriesService.getDataSet("BTC-EUR");

		// Building the map of strategies
		Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(timeSeries);

		// The analysis criterion
		AnalysisCriterion profitCriterion = new TotalProfitCriterion();

		TimeSeriesManager timeSeriesManager = new TimeSeriesManager(timeSeries);
		for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
			Strategy strategy = entry.getKey();
			String name = entry.getValue();
			TradingRecord tradingRecord = timeSeriesManager.run(strategy);
			double profit = profitCriterion.calculate(timeSeries, tradingRecord).doubleValue();
			System.out.println("\tProfit(%) for " + name + ": " + percent(profit));
		}
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
		System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");

	}

	
	
	@Test
	public void chooseBestForAllSecurities() {
		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		double highestProfitProfit = 0;
		String highestProfitNameProfit = "";
		String highestProfitNameStrategyProfit = "";

		double highestProfitNrTrade = 0;
		String highestProfitNameNrTrade = "";
		String highestProfitNameStrategyNrTrade = "";

		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
		// The analysis criterion
		AnalysisCriterion profitCriterion = new TotalProfitCriterion();
		// The analysis criterion
		AnalysisCriterion nrOfTradesCriterion = new NumberOfTradesCriterion();		
		
		for (TimeSeries timeSeries : timeSeriesList) {
			Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(timeSeries);
			TimeSeriesManager timeSeriesManager = new TimeSeriesManager(timeSeries);
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
			double highestProfitPercentageProfit = (highestProfitProfit - 1 );
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameProfit + " profit.format: " + format.format(highestProfitPercentageProfit));
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameProfit + " profit%: " + percent(highestProfitProfit));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategyProfit + "\n");

			Strategy bestNrOfTradesStrategy = nrOfTradesCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
			System.out.println("\t\t--> Best strategy(nr of trades) for : "+timeSeries.getName()+" = " + strategies.get(bestNrOfTradesStrategy));
			double highestProfitPercentageNrTrade = (highestProfitNrTrade - 1 );
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameNrTrade + " profit.format: " + format.format(highestProfitPercentageNrTrade));
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitNameNrTrade + " profit%: " + percent(highestProfitNrTrade));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategyNrTrade + "\n");
		}

	}	
	
	
	@Test
	public void percent() {
		double x = 1.0699432892249527;
		
		System.out.println("x="+x);
		
		 double xx = (x - 1 ); 
		 
		 System.out.println("xx="+xx);
		 
		 NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		
		 String xxx = format.format(xx);
		 
		 System.out.println("xxx="+xxx);

		System.out.println("yyy="+percent(x));


		
	}
	
	private String percent(double value) {
		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		format.setMinimumFractionDigits(1);

		double raw = (value - 1);

		return format.format(raw);
	}
	
}
