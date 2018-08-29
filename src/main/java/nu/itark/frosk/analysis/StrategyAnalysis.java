package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.AverageProfitCriterion;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.LinearTransactionCostCriterion;
import org.ta4j.core.analysis.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.analysis.criteria.NumberOfBarsCriterion;
import org.ta4j.core.analysis.criteria.NumberOfTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;

import nu.itark.frosk.dataset.TradeView;
import nu.itark.frosk.service.TimeSeriesService;
import nu.itark.frosk.strategies.RSI2Strategy;
/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service("strategyAnalysis")
public class StrategyAnalysis {
	Logger logger = Logger.getLogger(StrategyAnalysis.class.getName());
	
	@Autowired
	TimeSeriesService timeSeriesService;
	
	/**
	 * Load all TimeSeries defined by and run all available strategies.
	 * 
	 * @return List of FeaturedStrategyDTO
	 */
	public List<FeaturedStrategyDTO> runStrategy(String strategy) {
		logger.info("runStrategyMatrix("+strategy+")");

		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
		List<FeaturedStrategyDTO> fsList = new ArrayList<FeaturedStrategyDTO>();
		FeaturedStrategyDTO fs = null;
		List<Trade> trades = null;
		
        double totalProfit ;
        double totalProfitPercentage;
        ZonedDateTime latestTradeDate= null;
        Strategy strat = null;
        RSI2Strategy rsiStrat = null;
        
		for (TimeSeries series : timeSeriesList) {
			logger.info("RSI2Strategy.class.getSimpleName()="+RSI2Strategy.class.getSimpleName());
			if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
				rsiStrat = new RSI2Strategy(series);
				strat = rsiStrat.buildStrategy();
				
			}
			if (strat == null) {
				throw new RuntimeException("strat is null");
			}
			
			TimeSeriesManager seriesManager = new TimeSeriesManager(series);
			TradingRecord tradingRecord = seriesManager.run(strat);
			trades = tradingRecord.getTrades();

			List<TradeView> tradeViewList = new ArrayList<TradeView>();
			TradeView tr = null;

			for (Trade trade : trades) {
				Bar barEntry = series.getBar(trade.getEntry().getIndex());
				LocalDate buyDate = barEntry.getEndTime().toLocalDate();
				tr = new TradeView();
				tr.setDate(buyDate);
				tr.setType("B");
				tradeViewList.add(tr);

				Bar barExit = series.getBar(trade.getExit().getIndex());
				LocalDate sellDate = barExit.getEndTime().toLocalDate();
				tr = new TradeView();
				tr.setDate(sellDate);
				tr.setType("S");
				tradeViewList.add(tr);

				latestTradeDate = barEntry.getEndTime();

			}

			fs = new FeaturedStrategyDTO();
			fs.setName(strategy);
			fs.setSecurity(series.getName());
			fs.setPeriodDescription(getDate(series));
			fs.setLatestTradeDate(latestTradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
			totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
			totalProfitPercentage = (totalProfit - 1) * 100;
			fs.setTotalProfit(new BigDecimal(totalProfitPercentage).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord)));
			double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord);
			fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));

			fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord)));
			fs.setProfitableTradesRatio(
					String.valueOf(new AverageProfitableTradesCriterion().calculate(series, tradingRecord)));
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord);
			fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setRewardRiskRatio(String.valueOf((new RewardRiskRatioCriterion().calculate(series, tradingRecord))));
			fs.setTotalTranactionCost(
					new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord)));
			fs.setTrades(tradeViewList);
			fs.setIndicatorValues(rsiStrat.getValues());

			fsList.add(fs);

		}

		return fsList;
	}
	
	/**
	 * Load all TimeSeries defined by and run all available strategies.
	 * 
	 * @return List of FeaturedStrategyDTO
	 */
	public List<FeaturedStrategyDTO> runStrategyMatrix() {
		logger.info("runStrategyMatrix()");
		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
		List<FeaturedStrategyDTO> fsList = new ArrayList<FeaturedStrategyDTO>();
		FeaturedStrategyDTO fs = null;
		List<Trade> trades = null;
		
        double totalProfit ;
        double totalProfitPercentage;
        ZonedDateTime latestTradeDate= null;

		for (TimeSeries series : timeSeriesList) {
			Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(series);  //TODO; Hardcoded it for now
			for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
				Strategy strategy = entry.getKey();
				String name = entry.getValue();
		        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
		        TradingRecord tradingRecord = seriesManager.run(strategy);
		        
		        trades = tradingRecord.getTrades();
		        if (trades.isEmpty()) continue;
		        
		        List<TradeView>  tradeViewList = new ArrayList<TradeView>();
		        TradeView tr = null;

		        for (Trade trade : trades) {
		            // Buy signal
		            Bar barEntry = series.getBar(trade.getEntry().getIndex());
//		        	Decimal closePriceBuy = series.getBar(trade.getEntry().getIndex()).getClosePrice();
		            LocalDate buyDate = series.getBar(trade.getEntry().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(buyDate);
		            tr.setType("B");
		            tradeViewList.add(tr);
	            
		            // Sell signal
//		            Decimal closePriceSell = series.getBar(trade.getExit().getIndex()).getClosePrice();
		            LocalDate sellDate = series.getBar(trade.getExit().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(sellDate);
		            tr.setType("S");
		            tradeViewList.add(tr);

//		            Decimal profit = closePriceSell.minus(closePriceBuy);
		            
		            
		            latestTradeDate = barEntry.getEndTime();
		            
		        }		        
		        
		        fs = new FeaturedStrategyDTO();
				fs.setName(name);
				fs.setSecurity(series.getName());
				fs.setPeriodDescription(getDate(series));
				fs.setLatestTradeDate(latestTradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
				totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
				totalProfitPercentage = (totalProfit - 1 ) *100;
				fs.setTotalProfit(new BigDecimal(totalProfitPercentage).setScale(2, BigDecimal.ROUND_DOWN));
				fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord)));
				double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord);
				fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
	
				fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord)));
				fs.setProfitableTradesRatio(
						String.valueOf(new AverageProfitableTradesCriterion().calculate(series, tradingRecord)));
				double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord);
				fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
				fs.setRewardRiskRatio(
						String.valueOf((new RewardRiskRatioCriterion().calculate(series, tradingRecord))));
				fs.setTotalTranactionCost(new BigDecimal(
						new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord)));
				fs.setTrades(tradeViewList);
//				fs.setIndicatorValues(rsiStrat.);
				
				fsList.add(fs);

			}
		}

		return fsList;
	}

	private List<Bar> getIndicatorValues(Strategy strategy) {

		logger.info("getIndicatorValues, strategy="+strategy);
		
//		strategy.
		
		// TODO Auto-generated method stub
		return null;
	}

	private String getDate(TimeSeries series) {
	StringBuilder sb = new StringBuilder();
    if (!series.getBarData().isEmpty()) {
        Bar firstBar = series.getFirstBar();
        Bar lastBar = series.getLastBar();
        sb.append(firstBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        sb.append(" - ");
        sb.append(lastBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    return sb.toString();
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
