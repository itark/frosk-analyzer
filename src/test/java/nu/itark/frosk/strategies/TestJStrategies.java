package nu.itark.frosk.strategies;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.Bar;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.num.Num;

import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.service.TimeSeriesService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJStrategies {

	Logger logger = Logger.getLogger(TestJStrategies.class.getName());

	@Autowired
	TimeSeriesService timeSeriesService;
	
	
	@Test
	public void runAllSingle() {
		TimeSeries timeSeries = timeSeriesService.getDataSet("SSAB-B.ST");
		//RSI2
//		RSI2Strategy rsi = new RSI2Strategy(timeSeries);
//		run(rsi.buildStrategy(),timeSeries);
		
		//Moving Momentum
		MovingMomentumStrategy mm =  new MovingMomentumStrategy(timeSeries);
		run(mm.buildStrategy(),timeSeries);
		
		//Global Extrema
//		GlobalExtremaStrategy ge = new GlobalExtremaStrategy(timeSeries);
//		run(ge.buildStrategy(),timeSeries);
		
		//CCI Corrections
//		CCICorrectionStrategy cci = new CCICorrectionStrategy(timeSeries);
//		run(cci.buildStrategy(),timeSeries);
		
	}

	
	@Test
	public void runAllList() {
		List<TimeSeries> timeSeriesList=   timeSeriesService.getDataSet();
		timeSeriesList.forEach(ts -> {
			RSI2Strategy rsi = new RSI2Strategy(ts);
			run(rsi.buildStrategy(),ts);
			
		});
		//RSI2
//		RSI2Strategy rsi = new RSI2Strategy(timeSeries);
//		run(rsi.buildStrategy(),timeSeries);
		
	}	
	
	
	public final void run(Strategy strategy, TimeSeries timeSeries) {
		logger.info("::"+strategy.getName()+"::");
		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
		TradingRecord tradingRecord = seriesManager.run(strategy);
		List<Trade> trades = tradingRecord.getTrades();

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

		logger.info("Number of trades for the strategy: " + tradingRecord.getTradeCount());
		// Analysis
		logger.info("Total profit for the strategy: " + new TotalProfitCriterion().calculate(timeSeries, tradingRecord));
		double totalProfit = new TotalProfitCriterion().calculate(timeSeries, tradingRecord).doubleValue();
		double totalProfitPercentage = (totalProfit - 1) * 100; // TODO minus
		logger.info("Total profit for the strategy (%): " + totalProfitPercentage);
	}
	
	
	
	
	
	
	@Test
	public void chooseBestForSecurity() {
		// Splitting the series into slices
		TimeSeries timeSeries = timeSeriesService.getDataSet("BOL.ST");

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
	public void chooseBestForAllSecurity() {
		NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
		double highestProfit = 0;
		String highestProfitName = "";
		String highestProfitNameStrategy = "";
		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
		// The analysis criterion
		AnalysisCriterion profitCriterion = new TotalProfitCriterion();

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
//				System.out.println(
//						"\tProfit for strategy: " + name + " and security :" + timeSeries.getName() + " = " + profit);
			}
			Strategy bestStrategy = profitCriterion.chooseBest(timeSeriesManager,
					new ArrayList<Strategy>(strategies.keySet()));
			System.out.println("\t\t--> Best strategy for : "+timeSeries.getName()+" = " + strategies.get(bestStrategy));
			 double highestProfitPercentage = (highestProfit - 1 );  //TODO minus
			System.out.println("\t\t--> HighestProfit Name: " + highestProfitName + " profit: " + format.format(highestProfitPercentage));
			System.out.println("\t\t--> Strategy: " + highestProfitNameStrategy + "\n");

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
	
}
