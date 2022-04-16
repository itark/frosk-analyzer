package nu.itark.frosk.strategies;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.service.TimeSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.NumberOfBarsCriterion;
import org.ta4j.core.analysis.criteria.NumberOfTradesCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.num.Num;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategies {

	Logger logger = Logger.getLogger(TestJStrategies.class.getName());

	@Autowired
	TimeSeriesService timeSeriesService;
	
	
	@Test
	public void runAllSingleDataSet() {
//		TimeSeries timeSeries = timeSeriesService.getDataSet("SSAB-B.ST");
//		TimeSeries timeSeries = timeSeriesService.getDataSet("KINV-B.ST");
//		TimeSeries timeSeries = timeSeriesService.getDataSet("BOL.ST");
		//TimeSeries timeSeries = timeSeriesService.getDataSet("MAV.ST");
		TimeSeries timeSeries = timeSeriesService.getDataSetFromCoinbase("BTC-EUR");

//		RSI2Strategy rsi = new RSI2Strategy(timeSeries);
//		run(rsi.buildStrategy(),timeSeries);
		
//		MovingMomentumStrategy mm =  new MovingMomentumStrategy(timeSeries);
//		run(mm.buildStrategy(),timeSeries);

		SimpleMovingMomentumStrategy smm =  new SimpleMovingMomentumStrategy(timeSeries);
		run(smm.buildStrategy(),timeSeries);

//		GlobalExtremaStrategy ge1 = new GlobalExtremaStrategy(timeSeries);
//		run(ge1.buildStrategy(),timeSeries);
//		
//		GlobalExtremaStrategy ge2 = new GlobalExtremaStrategy(timeSeries);
//		run(ge2.buildStrategy(2),timeSeries);

//		GlobalExtremaStrategy ge3 = new GlobalExtremaStrategy(timeSeries);
//		run(ge3.buildStrategy(2,2),timeSeries);
		
//		CCICorrectionStrategy cci = new CCICorrectionStrategy(timeSeries);
//		run(cci.buildStrategy(),timeSeries);
		
//		EngulfingStrategy eng = new EngulfingStrategy(timeSeries);
//		run(eng.buildStrategy(),timeSeries);
		
//		HaramiStrategy harami = new HaramiStrategy(timeSeries);
//		run(harami.buildStrategy(),timeSeries);	
	
//		ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(timeSeries);
//		run(three.buildStrategy(),timeSeries);

//		ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(timeSeries);
//		run(cd.buildStrategy(),timeSeries);
		
		
	}

	
	@Test
	public void runAllDataSetList() {
		List<TimeSeries> timeSeriesList=   timeSeriesService.getDataSet();
		timeSeriesList.forEach(ts -> {
//			RSI2Strategy rsi = new RSI2Strategy(ts);
//			run(rsi.buildStrategy(),ts);
			
//			ThreeBlackWhiteStrategy three = new ThreeBlackWhiteStrategy(ts);
//			run(three.buildStrategy(),ts);		
			
			ConvergenceDivergenceStrategy cd = new ConvergenceDivergenceStrategy(ts);
			run(cd.buildStrategy(),ts);			
			
			
			
		});
		//RSI2
//		RSI2Strategy rsi = new RSI2Strategy(timeSeries);
//		run(rsi.buildStrategy(),timeSeries);
		
	}	
	
	
	public final void run(Strategy strategy, TimeSeries timeSeries) {
		logger.info("******"+strategy.getName()+"******");
		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
		TradingRecord tradingRecord = seriesManager.run(strategy);
		List<Trade> trades = tradingRecord.getTrades();

		boolean currentTradeIsOpened = tradingRecord.getCurrentTrade().isOpened();

		System.out.println("currentTradeIsOpened:"+currentTradeIsOpened);

		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());


		for (Trade trade : trades) {
			Bar barEntry = timeSeries.getBar(trade.getEntry().getIndex());
			logger.info(timeSeries.getName() + "::barEntry=" + barEntry.getDateName());
			Bar barExit = timeSeries.getBar(trade.getExit().getIndex());
			logger.info(timeSeries.getName() + "::barExit=" + barExit.getDateName());
			Num closePriceBuy = barEntry.getClosePrice();
			Num closePriceSell = barExit.getClosePrice();
			Num profit = closePriceSell.minus(closePriceBuy);

			logger.info("profit=" + profit);

		}

		
	      // Total profit
        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
        logger.info("********Total profit: " + percent(totalProfit.calculate(timeSeries, tradingRecord).doubleValue())+ "********");
        
        // Number of bars
        logger.info("Number of bars: " + new NumberOfBarsCriterion().calculate(timeSeries, tradingRecord));
        // Average profit (per bar)
//        logger.info("Average profit (per bar): " + percent(new AverageProfitCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Number of trades
        logger.info("Number of trades: " + new NumberOfTradesCriterion().calculate(timeSeries, tradingRecord));
        // Profitable trades ratio
//        logger.info("Profitable trades ratio: " + percent(new AverageProfitableTradesCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Maximum drawdown
//        logger.info("Maximum drawdown: " + percent(new MaximumDrawdownCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Reward-risk ratio
//        logger.info("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(timeSeries, tradingRecord));
        // Total transaction cost
//        logger.info("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(timeSeries, tradingRecord));
        // Buy-and-hold
//        logger.info("Buy-and-hold: " + percent(new BuyAndHoldCriterion().calculate(timeSeries, tradingRecord).doubleValue()));
        // Total profit vs buy-and-hold
        logger.info("Custom strategy profit vs buy-and-hold strategy profit: " + percent(new VersusBuyAndHoldCriterion(totalProfit).calculate(timeSeries, tradingRecord).doubleValue()));		
		
		
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
			System.out.println("\tProfit for " + name + ": " + profit);
		}
		Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
		System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");

	}

	
	
	@Test
	public void chooseBestForAllSecurities() {
		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		double highestProfit = 0;
		String highestProfitName = "";
		String highestProfitNameStrategy = "";
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
				double profit = profitCriterion.calculate(timeSeries, tradingRecord).doubleValue();
				if (profit > highestProfit) {
					highestProfit = profit;
					highestProfitName = timeSeries.getName();
					highestProfitNameStrategy = strategy.getName();
				}
				
				
//				double profit = nrOfTradesCriterion.calculate(timeSeries, tradingRecord).doubleValue();
//				if (profit > highestProfit) {
//					highestProfit = profit;
//					highestProfitName = timeSeries.getName();
//					highestProfitNameStrategy = strategy.getName();
//				}				
				
				
				
//				System.out.println(
//						"\tProfit for strategy: " + name + " and security :" + timeSeries.getName() + " = " + profit);
			}
			Strategy bestProfitStrategy = profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
			System.out.println("\t\t--> Best strategy(profit criteria) for : "+timeSeries.getName()+" = " + strategies.get(bestProfitStrategy));
			 double highestProfitPercentage = (highestProfit - 1 );  //TODO minus
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitName + " profit: " + format.format(highestProfitPercentage));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategy + "\n");

//			Strategy bestNrOfTradesStrategy = nrOfTradesCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies.keySet()));
//			System.out.println("\t\t--> Best strategy(nr of trades) for : "+timeSeries.getName()+" = " + strategies.get(bestNrOfTradesStrategy));
//			 double highestProfitPercentage = (highestProfit - 1 );  //TODO minus
//			System.out.println("\t\t--> HighestProfit Name: " + highestProfitName + " profit: " + format.format(highestProfitPercentage));
//			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategy + "\n");

		
		
		
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
		
	}
	
	private String percent(double value) {
		NumberFormat format = NumberFormat.getPercentInstance();
		format.setMinimumFractionDigits(1);

		double raw = (value - 1);

		return format.format(raw);
	}
	
}
