package nu.itark.frosk.service;


import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.strategies.prediction.workday.ArimaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.LinearBorrowingCostModel;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.backtest.TradeExecutionModel;
import org.ta4j.core.backtest.TradeOnCurrentCloseModel;
import org.ta4j.core.backtest.TradeOnNextOpenModel;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BarSeriesService  {

	@Value("${exchange.transaction.feePerTradePercent}")
	private double feePerTradePercent;

	@Value("${exchange.transaction.initialAmount}")
	private double initialAmount;

	@Value("${frosk.strategy.buy:false}")
	private boolean isBuy;

	@Value("${frosk.strategy.buy.amount:true}")
	private boolean isBuyAmount;

	@Autowired
	SecurityPriceRepository securityPriceRepository;	
	
	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	TradingAccountService tradingAccountService;

	@Autowired
	ProductProxy productProxy;

	ArimaModel arimaModel = new ArimaModel();

	/**
	 * Retrive from {@linkplain SecurityPriceRepository}
	 * 
	 * @return List<BarSeries> for alla securities in database. Filter on 'EUR'
	 */
	public List<BarSeries> getDataSet() {
		Iterable<Security> spList = securityRepository.findAllByActiveAndQuoteCurrency(true, "EUR");
		List<BarSeries> barSeries = new ArrayList<BarSeries>();
		
		spList.forEach(sp -> {
			barSeries.add(getDataSet( getSecurityId(sp.getName())  ));
		});
		
		return barSeries;
		
	}	

	
	public Long getSecurityId(String securityName) {
		return securityRepository.findByName(securityName).getId();
	}
	
	/**
	 * Return BarSeries bases on name in Security.
	 *
	 * @param  {@linkplain Security}
	 * @param  api, true if retrieving data direcly from coinbase api.
	 * @return BarSeries
	 */
	public BarSeries getDataSet(String securityName, boolean api, boolean forecast) {
		BarSeries barSeries;
		if (api) {
			barSeries=  getDataSetFromCoinbase(securityName);
		} else {
			barSeries=  getDataSet( getSecurityId(securityName)  );
		}
		if (forecast) {
			return withArimaForecast(barSeries, 3);
		} else {
			return barSeries;
		}
	}

	public BarSeries withArimaForecast(BarSeries barSeries, int forecastSize) {
		BarSeries seriesWithForecast = barSeries;

		double[] closePrices = new double[barSeries.getBarCount()];
		double[] highPrices = new double[barSeries.getBarCount()];
		double[] lowPrices = new double[barSeries.getBarCount()];
		double[] openPrices = new double[barSeries.getBarCount()];
		double[] volumes = new double[barSeries.getBarCount()];

		for (int i = 0; i < barSeries.getBarCount(); i++) {
			closePrices[i] = barSeries.getBar(i).getClosePrice().doubleValue();
			highPrices[i] = barSeries.getBar(i).getHighPrice().doubleValue();
			lowPrices[i] = barSeries.getBar(i).getLowPrice().doubleValue();
			openPrices[i] = barSeries.getBar(i).getOpenPrice().doubleValue();
			volumes[i] = barSeries.getBar(i).getVolume().doubleValue();
		}

		double[] forecastedClosePrice = arimaModel.forecast(closePrices, forecastSize);
		double[] forecastedHighPrices = arimaModel.forecast(highPrices, forecastSize);
		double[] forecastedLowPrices = arimaModel.forecast(lowPrices, forecastSize);
		double[] forecastedOpenPrices = arimaModel.forecast(openPrices, forecastSize);
		double[] forecastedVolumes = arimaModel.forecast(volumes, forecastSize);

		for (int i = 0; i < forecastSize; i++) {
			seriesWithForecast.addBar(barSeries.getLastBar().getEndTime().plusDays(i+1), forecastedOpenPrices[i], forecastedHighPrices[i], forecastedLowPrices[i], forecastedClosePrice[i], forecastedVolumes[i]);
		}

		return seriesWithForecast;
	}

	/**
	 * Return TimesSeries bases on id in Security.
	 * 
	 * @param security_id in {@linkplain Security}
	 * @return BarSeries
	 */
	public BarSeries getDataSet(Long security_id) {
		Optional<Security> security = securityRepository.findById(security_id);
		//Sanity check
		if (security == null){
			throw new RuntimeException("Security is null");
		}

		BarSeries series = new BaseBarSeriesBuilder().withName(security.get().getName()).withNumTypeOf(DoubleNum.class).build();
		//BarSeries series = new BaseBarSeriesBuilder().withName(security.get().getName()).withNumTypeOf(DecimalNum.class).build();

		List<SecurityPrice> securityPrices =securityPriceRepository.findBySecurityIdOrderByTimestamp(security.get().getId());
		
		securityPrices.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());		
		     series.addBar(dateTime, row.getOpen(), row.getHigh(),  row.getLow(), row.getClose(), row.getVolume());			
		});
		
		return series;
		
	}	
	

	/**
	 * Return TimesSeries bases on productId in Coinbase.
	 * 
	 * @param productId
	 * @return BarSeries
	 *
	 */
	public BarSeries getDataSetFromCoinbase(String productId) {
		BarSeries series = new BaseBarSeriesBuilder().withName(productId).withNumTypeOf(DecimalNum.class).build();
  		Candles candles = productProxy.getCandles(productId, SelectionCriteria.startTime,SelectionCriteria.endTime, SelectionCriteria.granularity );
		List<Candle> sortedList = candles.getCandles()
				.stream()
				.sorted((p1, p2)-> p1.getStart().compareTo(p2.getStart()))
				.collect(Collectors.toList());

		sortedList.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getStart(),ZoneId.systemDefault());
			series.addBar(dateTime, row.getOpen(), row.getHigh(), row.getLow(), row.getClose(), row.getVolume());
		});

		return series;

	}

	public TradingRecord runConfiguredStrategy(BarSeries barSeries, Strategy strategyToRun) {
		//TODO
		double borrowingFee = 0.00001;
		CostModel transactionCostModel = new LinearTransactionCostModel(feePerTradePercent);
		CostModel borrowingCostModel = new LinearBorrowingCostModel(borrowingFee);
		TradeExecutionModel tradeExecutionModel;
		if ("EngulfingStrategy".equals(strategyToRun.getName())) {
			tradeExecutionModel = new TradeOnCurrentCloseModel();
		} else {
			tradeExecutionModel = new TradeOnNextOpenModel();
		}
		//TradeExecutionModel tradeExecutionModel = new TradeOnNextOpenModel();
		//TradeExecutionModel tradeExecutionModel = new TradeOnCurrentCloseModel();
		BarSeriesManager seriesManager = new BarSeriesManager(barSeries, transactionCostModel, borrowingCostModel, tradeExecutionModel);
		if (isBuy) {
			return seriesManager.run(strategyToRun, Trade.TradeType.BUY);
		} else if (isBuyAmount) {
			return seriesManager.run(strategyToRun, Trade.TradeType.BUY, getAmount(barSeries));
		}
		return seriesManager.run(strategyToRun);
	}

	public Num getAmount(BarSeries barSeries) {
		TradingAccount tradingAccount = tradingAccountService.getDefaultActiveTradingAccount();
		final BigDecimal positionValue = tradingAccount.getPositionValue();
		return DoubleNum.valueOf(positionValue).dividedBy(barSeries.getFirstBar().getClosePrice());
	}

	public static class SelectionCriteria {
		private static Instant startTime = Instant.now().minus(300, ChronoUnit.DAYS);
		private static Instant endTime = Instant.now();
		private static Granularity granularity = Granularity.ONE_DAY;
	}

	
}
