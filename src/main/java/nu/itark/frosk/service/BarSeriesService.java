package nu.itark.frosk.service;


import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
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

	public List<BarSeries> getDataSet(Database database) {
		//Iterable<Security> securities = securityRepository.findByDatabase(database.name());
		Iterable<Security> securities = securityRepository.findByDatabaseAndActive(database.name(), true);
		List<BarSeries> barSeries = new ArrayList<BarSeries>();
		
		securities.forEach(security -> {
			barSeries.add(getDataSet(security.getId()));
		});
		return barSeries;
	}


	public Long getSecurityId(String securityName) {
		final Security byName = securityRepository.findByName(securityName);
		if (byName == null) {
			throw new RuntimeException("Could not find Security: "+securityName+ " in database");
		}
		return byName.getId();
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
			return null; //TODO
		} else {
			return barSeries;
		}
	}

	/**
	 * Return TimesSeries bases on id in Security.
	 * 
	 * @param security_id in {@linkplain Security}
	 * @return BarSeries
	 */
	public BarSeries getDataSet(Long security_id) {
		//log.info("Getting dataset for security_id={}", security_id);
		Security security = securityRepository.findById(security_id).orElse(null);
		BarSeries series = new BaseBarSeriesBuilder().withName(security_id.toString()).withNumTypeOf(DoubleNum.class).build();
		//log.info("Database call...on security_id={}, security_name={}", security.getId(), security.getName());
		List<SecurityPrice> securityPrices =securityPriceRepository.findBySecurityIdOrderByTimestamp(security_id);
		//log.info("Database call ready.");
		securityPrices.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());
		     series.addBar(dateTime, row.getOpen(), row.getHigh(),  row.getLow(), row.getClose(), row.getVolume());
		});
		//log.info("Returning dataset for security_id={}", security_id);
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
		}
		else if ("GoldStrategy".equals(strategyToRun.getName())) {
			tradeExecutionModel = new TradeOnCurrentCloseModel();
		}
		else {
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
