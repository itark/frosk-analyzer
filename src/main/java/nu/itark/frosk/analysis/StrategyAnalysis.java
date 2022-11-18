package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.*;


import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.TradesRepository;
import org.ta4j.core.analysis.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.analysis.criteria.pnl.NetProfitCriterion;
import org.ta4j.core.analysis.criteria.pnl.ProfitLossPercentageCriterion;

/**
 * This class diplays analysis criterion values after running a trading strategy
 * over a time series.
 */
@Service
public class StrategyAnalysis {
	Logger logger = Logger.getLogger(StrategyAnalysis.class.getName());
	
	@Autowired
	BarSeriesService barSeriesService;
	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;
	
	@Autowired
	TradesRepository tradesRepository;
	
	@Autowired
	StrategyIndicatorValueRepository indicatorValueRepo;
	
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
		logger.info("run("+strategy+", "+security_id+")");
	
		if (Objects.isNull(strategy)  && Objects.isNull(security_id)) {
			List<String> strategies = StrategiesMap.buildStrategiesMap();
			strategies.forEach(strategyName -> {
				try {
					runStrategy(strategyName, barSeriesService.getDataSet());
				} catch (DataIntegrityViolationException e) {
					logger.severe("Error runStrategy on strategyName="+ strategyName);
					throw e;
				}
			});
			
		} 
		else if (Objects.nonNull(strategy) && Objects.isNull(security_id)) {
			try {
				runStrategy(strategy, barSeriesService.getDataSet());
			} catch (Exception e) {
				logger.severe("Error runStrategy on strategy="+ strategy);
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
				logger.severe("Error runStrategy on strategy="+ strategy+ " and security_id="+security_id);
				throw e;
			}
		} 
		else {
			throw new UnsupportedOperationException("kalle anka");
		}
		
	}
	
	private void runStrategy(String strategy, List<BarSeries> BarSeriesList) throws DataIntegrityViolationException{
		FeaturedStrategy fs = null;
		List<Position> positions = null;
        double totalProfit ;
        double totalProfitPercentage;
        Date latestTradeDate= null;
        Strategy strategyToRun = null;
        
		for (BarSeries series : BarSeriesList) {
			logger.info("runStrategy("+strategy+", "+series.getName()+")");
			strategyToRun = getStrategyToRun(strategy, series);
			BarSeriesManager seriesManager = new BarSeriesManager(series);
			TradingRecord tradingRecord = seriesManager.run(strategyToRun);
			positions = tradingRecord.getPositions();
			if (series.getBarData().isEmpty()){
				//abort
				return;
			}
			Set<StrategyTrade> strategyTradeList = new HashSet<StrategyTrade>();
			StrategyTrade strategyTrade = null;

			for (Position trade : positions) {
				Bar barEntry = series.getBar(trade.getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = trade.getEntry().getType().name();
				strategyTrade = new StrategyTrade(buyDate,entryType,BigDecimal.valueOf(barEntry.getLowPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);

				Bar barExit = series.getBar(trade.getExit().getIndex());
				Date sellDate = Date.from(barExit.getEndTime().toInstant());
				String exitType = trade.getExit().getType().name();
				strategyTrade = new StrategyTrade(sellDate,exitType,BigDecimal.valueOf(barExit.getLowPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barExit.getEndTime().toInstant());
			}

			//If open
			if (tradingRecord.getCurrentPosition().isOpened()) {
				Bar barEntry = series.getBar(tradingRecord.getCurrentPosition().getEntry().getIndex());
				Date buyDate = Date.from(barEntry.getEndTime().toInstant());
				String entryType = tradingRecord.getCurrentPosition().getEntry().getType().name();
				strategyTrade = new StrategyTrade(buyDate,entryType,BigDecimal.valueOf(barEntry.getLowPrice().doubleValue()));
				strategyTradeList.add(strategyTrade);
				latestTradeDate = Date.from(barEntry.getEndTime().toInstant());
			}

			fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, series.getName());
			if (fs == null) {  //=new run
				fs = new FeaturedStrategy();
				fs.setName(strategy);
				fs.setSecurityName(series.getName());
			}

			fs.setPeriod(getPeriod(series));
			fs.setLatestTrade(latestTradeDate);
			totalProfit = new GrossReturnCriterion().calculate(series, tradingRecord).doubleValue();
			//totalProfit = new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setTotalProfit(new BigDecimal(totalProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberOfTicks(new BigDecimal(new NumberOfBarsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double averageTickProfit = new AverageProfitCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setAverageTickProfit(new BigDecimal(averageTickProfit).setScale(2, BigDecimal.ROUND_DOWN));
			fs.setNumberofTrades(new BigDecimal(new NumberOfPositionsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
			double profitableTradesRatio = new WinningPositionsRatioCriterion().calculate(series, tradingRecord).doubleValue();
			if (!Double.isNaN(profitableTradesRatio)) {
				fs.setProfitableTradesRatio(new BigDecimal(profitableTradesRatio).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
			fs.setMaxDD(new BigDecimal(maximumDrawdownCriterion).setScale(2, BigDecimal.ROUND_DOWN));
/*
			double rewardRiskRatio = new ValueAtRiskCriterion(2D).calculate(series, tradingRecord).doubleValue();
			if (Double.isFinite(rewardRiskRatio)) {
				fs.setRewardRiskRatio(new BigDecimal(rewardRiskRatio).setScale(2, BigDecimal.ROUND_DOWN));
			}
*/
			double buyAndHold = new BuyAndHoldReturnCriterion().calculate(series, tradingRecord).doubleValue();
			if (Double.isFinite(buyAndHold)) {
				fs.setBuyAndHold(new BigDecimal(buyAndHold).setScale(2, BigDecimal.ROUND_DOWN));
			}
			double totalProfitVsButAndHold = new VersusBuyAndHoldCriterion(new NetProfitCriterion()).calculate(series, tradingRecord).doubleValue();

			if (Double.isFinite(totalProfitVsButAndHold)) {
				fs.setTotalProfitVsButAndHold(new BigDecimal(totalProfitVsButAndHold).setScale(2, BigDecimal.ROUND_DOWN));
			}
			fs.setTotalTransactionCost(
					new BigDecimal(new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord).doubleValue()));
			fs.setOpen(tradingRecord.getCurrentPosition().isOpened());

			FeaturedStrategy fsRes = featuredStrategyRepository.saveAndFlush(fs);
			//Trades
			List<StrategyTrade>  existSt = tradesRepository.findByFeaturedStrategyId(fsRes.getId());
			if (!existSt.isEmpty()) {
				existSt.forEach(st -> {
					tradesRepository.delete(st);
					//tradesRepository.saveAndFlush(st);
				});
			}
			strategyTradeList.forEach(st -> {
				st.setFeaturedStrategy(fsRes);
				tradesRepository.saveAndFlush(st);
			});

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

			logger.info("EXIT runStrategy("+strategy+", "+series.getName()+")");

		}
	}

	public List<StrategyIndicatorValue> getIndicatorValues(String strategy, BarSeries series) {
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
		} else {
			throw new RuntimeException("Strategy not found!, strategy="+strategy);
		}

	}
	
	public Strategy getStrategyToRun(String strategy,  BarSeries series) {
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
		}
		else {
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
