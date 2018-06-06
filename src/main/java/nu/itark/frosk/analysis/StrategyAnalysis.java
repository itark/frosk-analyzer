package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.Decimal;
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
/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service("strategyAnalysis")
public class StrategyAnalysis {
	Logger logger = Logger.getLogger(StrategyAnalysis.class.getName());
	
	@Autowired
	TimeSeriesService timeSeriesService;
	
//	public List<FeaturedStrategyDTO> runStrategyMatrixXXX(String strategy) {
//		logger.info("runStrategyMatrix("+strategy+")");
//
//		Strategy buildStrategy;
//		String stratName;
//		List<TimeSeries> timeSeriesList = timeSeriesService.getDataSet();
//		
//		List<FeaturedStrategyDTO> fsList = new ArrayList<FeaturedStrategyDTO>();
//		FeaturedStrategyDTO fs = null;
//		List<Trade> trades = null;
//		
//        double totalProfit ;
//        double totalProfitPercentage;
//
//        
//		for (TimeSeries series : timeSeriesList) {
//				if (strategy.equals(RSI2Strategy.class.getSimpleName())) {
//					buildStrategy = RSI2Strategy.buildStrategy(series);
//					stratName = RSI2Strategy.class.getSimpleName();
//				} else if (strategy.equals("ALL")) {
////					return runStrategyMatrix();
//				} else {
//					throw new RuntimeException(strategy +" is not supported!");
//				}
//				
//				TimeSeriesManager seriesManager = new TimeSeriesManager(series);
//		        TradingRecord tradingRecord = seriesManager.run(buildStrategy);
//		        
//		        trades = tradingRecord.getTrades();
//		        
//		        List<TradeView>  tradeViewList = new ArrayList<TradeView>();
//		        TradeView tr = null;
//
//		        for (Trade trade : trades) {
//		            // Buy signal
//		            Decimal closePriceBuy = series.getBar(trade.getEntry().getIndex()).getClosePrice();
//		            LocalDate buyDate = series.getBar(trade.getEntry().getIndex()).getEndTime().toLocalDate();
//		            tr = new TradeView();
//		            tr.setDate(buyDate);
//		            tr.setType("B");
//		            tradeViewList.add(tr);
//	
//		            // Sell signal
//		            Decimal closePriceSell = series.getBar(trade.getExit().getIndex()).getClosePrice();
//		            LocalDate sellDate = series.getBar(trade.getExit().getIndex()).getEndTime().toLocalDate();
//		            tr = new TradeView();
//		            tr.setDate(sellDate);
//		            tr.setType("S");
//		            tradeViewList.add(tr);
//
//		            Decimal profit = closePriceSell.minus(closePriceBuy);
//		            
//		        }		        
//		        
//		        fs = new FeaturedStrategyDTO();
//				fs.setName(stratName);
//				fs.setSecurity(series.getName());
//				fs.setPeriodDescription(getDate(series));
//
//				totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
//				totalProfitPercentage = (totalProfit - 1 ) *100;
//				fs.setTotalProfit(new BigDecimal(totalProfitPercentage));
//				fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord)));
//				fs.setAverageTickProfit(new BigDecimal(new AverageProfitCriterion().calculate(series, tradingRecord)));
//				fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord)));
//				fs.setProfitableTradesRatio(
//						String.valueOf(new AverageProfitableTradesCriterion().calculate(series, tradingRecord)));
//				fs.setMaxDD(new BigDecimal(new MaximumDrawdownCriterion().calculate(series, tradingRecord)));
//				fs.setRewardRiskRatio(
//						String.valueOf((new RewardRiskRatioCriterion().calculate(series, tradingRecord))));
//				fs.setTotalTranactionCost(new BigDecimal(
//						new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord)));
////				fs.setBuyAndHold(new BigDecimal(new BuyAndHoldCriterion().calculate(series, tradingRecord)));
//				fs.setTrades(tradeViewList);
//				
//				fsList.add(fs);
//
//		}
//
//		return fsList;
//	}
	
	
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

		for (TimeSeries series : timeSeriesList) {
			Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(series);  //Hardcode it for now
			for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
				Strategy strategy = entry.getKey();
				String name = entry.getValue();
		        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
		        TradingRecord tradingRecord = seriesManager.run(strategy);
		        
		        trades = tradingRecord.getTrades();
		        
		        List<TradeView>  tradeViewList = new ArrayList<TradeView>();
		        TradeView tr = null;

		        for (Trade trade : trades) {
		            // Buy signal
		            Decimal closePriceBuy = series.getBar(trade.getEntry().getIndex()).getClosePrice();
		            LocalDate buyDate = series.getBar(trade.getEntry().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(buyDate);
		            tr.setType("B");
		            tradeViewList.add(tr);
	            
		            // Sell signal
		            Decimal closePriceSell = series.getBar(trade.getExit().getIndex()).getClosePrice();
		            LocalDate sellDate = series.getBar(trade.getExit().getIndex()).getEndTime().toLocalDate();
		            tr = new TradeView();
		            tr.setDate(sellDate);
		            tr.setType("S");
		            tradeViewList.add(tr);

		            Decimal profit = closePriceSell.minus(closePriceBuy);
		            
		        }		        
		        
		        
		        fs = new FeaturedStrategyDTO();
				fs.setName(name);
				fs.setSecurity(series.getName());
				fs.setPeriodDescription(getDate(series));

				totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
				totalProfitPercentage = (totalProfit - 1 ) *100;
				fs.setTotalProfit(new BigDecimal(totalProfitPercentage));
				fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord)));
				fs.setAverageTickProfit(new BigDecimal(new AverageProfitCriterion().calculate(series, tradingRecord)));
				fs.setNumberofTrades(new BigDecimal(new NumberOfTradesCriterion().calculate(series, tradingRecord)));
				fs.setProfitableTradesRatio(
						String.valueOf(new AverageProfitableTradesCriterion().calculate(series, tradingRecord)));
				fs.setMaxDD(new BigDecimal(new MaximumDrawdownCriterion().calculate(series, tradingRecord)));
				fs.setRewardRiskRatio(
						String.valueOf((new RewardRiskRatioCriterion().calculate(series, tradingRecord))));
				fs.setTotalTranactionCost(new BigDecimal(
						new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord)));
//				fs.setBuyAndHold(new BigDecimal(new BuyAndHoldCriterion().calculate(series, tradingRecord)));
	/*
				fs.setTotalProfitVsButAndHold(
						new BigDecimal(new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord)));
*/
				fs.setTrades(tradeViewList);
				
				fsList.add(fs);

			}
		}

		return fsList;
	}

//	public List<TradeView> getTrades(String strategyName, String securityName) {
//		List<TradeView> tradeViewList = null;
//		//TODO Remove "double-run of runStrategyMatrix"
//
//		for (Iterator<FeaturedStrategyDTO> iterator = runStrategyMatrix(strategyName).iterator(); iterator.hasNext();) {  
//			FeaturedStrategyDTO featuredStrategyDTO = (FeaturedStrategyDTO) iterator.next();
//			if ( featuredStrategyDTO.getName().equals(strategyName)  && featuredStrategyDTO.getSecurity().equals(securityName) ) {
//				tradeViewList = featuredStrategyDTO.getTrades();
//			}
//		}
//
//		
//		
//		return tradeViewList;  
//	}
	
	public List<TradeView> getTrades(String strategyName, String securityName) {
		List<TradeView> tradeViewList = null;
		//TODO Remove "double-run of runStrategyMatrix"

//		for (Iterator<FeaturedStrategyDTO> iterator = runStrategyMatrix(strategyName).iterator(); iterator.hasNext();) {  
//			FeaturedStrategyDTO featuredStrategyDTO = (FeaturedStrategyDTO) iterator.next();
//			if ( featuredStrategyDTO.getName().equals(strategyName)  && featuredStrategyDTO.getSecurity().equals(securityName) ) {
//				tradeViewList = featuredStrategyDTO.getTrades();
//			}
//		}

		
		
		return tradeViewList;  
	}	
	
	
	
//	static Map<Strategy, String> buildStrategiesMap(TimeSeries series) {
//		HashMap<Strategy, String> strategies = new HashMap<>();
//		strategies.put(CCICorrectionStrategy.buildStrategy(series), CCICorrectionStrategy.class.getSimpleName());
//		strategies.put(GlobalExtremaStrategy.buildStrategy(series), GlobalExtremaStrategy.class.getSimpleName());
//		strategies.put(MovingMomentumStrategy.buildStrategy(series), MovingMomentumStrategy.class.getSimpleName());
//		strategies.put(RSI2Strategy.buildStrategy(series), RSI2Strategy.class.getSimpleName());
//		return strategies;
//	}

	
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
