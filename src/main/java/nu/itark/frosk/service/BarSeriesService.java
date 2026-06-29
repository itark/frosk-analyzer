package nu.itark.frosk.service;


import lombok.NonNull;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BarSeriesService  {

	@Value("${exchange.transaction.feePerTradePercent}")
	private double feePerTradePercent;

	@Value("${exchange.transaction.intradayFeePerTradePercent:0.0003}")
	private double intradayFeePerTradePercent;

	@Value("${exchange.transaction.cryptoTakerFeePerTradePercent:0.006}")
	private double cryptoTakerFeePerTradePercent;

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
		@NonNull Iterable<Security> securities = securityRepository.findByDatabaseAndActive(database.name(), true);
		List<Security> securityList = new ArrayList<>();
		securities.forEach(s -> {
			String n = s.getName();
			if (n != null && (n.startsWith("^") || n.endsWith("=X") || n.endsWith("=F"))) {
				log.debug("BarSeriesService: skipping non-tradeable instrument {}", n);
				return;
			}
			securityList.add(s);
		});

		// Batch-load all prices in a single query instead of N individual queries
		List<Long> ids = securityList.stream().map(Security::getId).collect(Collectors.toList());
		List<SecurityPrice> allPrices = securityPriceRepository.findBySecurityIdInOrderBySecurityIdAscTimestampAsc(ids);

		// Group prices by security_id
		Map<Long, List<SecurityPrice>> pricesBySecurityId = allPrices.stream()
				.collect(Collectors.groupingBy(SecurityPrice::getSecurityId));

		List<BarSeries> barSeriesList = new ArrayList<>();
		for (Security security : securityList) {
			BarSeries series = new BaseBarSeriesBuilder().withName(String.valueOf(security.getId())).withNumTypeOf(DoubleNum.class).build();
			List<SecurityPrice> prices = pricesBySecurityId.getOrDefault(security.getId(), Collections.emptyList());
			prices.stream()
				.filter(row -> row.getClose() != null && row.getClose().compareTo(BigDecimal.ZERO) > 0)
				.forEach(row -> {
					ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(), ZoneId.systemDefault());
					series.addBar(dateTime, row.getOpen(), row.getHigh(), row.getLow(), row.getClose(), row.getVolume());
				});
			barSeriesList.add(series);
		}
		return barSeriesList;
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
		securityPrices.stream()
			.filter(row -> row.getClose() != null && row.getClose().compareTo(BigDecimal.ZERO) > 0)
			.forEach(row -> {
				ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(), ZoneId.systemDefault());
				series.addBar(dateTime, row.getOpen(), row.getHigh(), row.getLow(), row.getClose(), row.getVolume());
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

	private static final java.util.Set<String> INTRADAY_STRATEGIES = java.util.Set.of(
			"OpeningRangeBreakoutIntradayStrategy", "VWAPMeanReversionIntradayStrategy", "GapReversalIntradayStrategy"
	);

	/**
	 * Crypto intraday strategies are backtested with the Coinbase taker fee
	 * (0.6%/trade) — NOT the 0.03% equity intraday broker fee, which would
	 * overstate crypto results by a factor of ~20. Add new crypto intraday
	 * strategy class names here.
	 */
	private static final java.util.Set<String> CRYPTO_INTRADAY_STRATEGIES = java.util.Set.of(
			"CryptoRangeBreakoutIntradayStrategy", "CryptoVWAPReversionIntradayStrategy"
	);

	private static final java.util.Set<String> CURRENT_CLOSE_STRATEGIES = java.util.Set.of(
			"EngulfingStrategy", "GoldStrategy",
			"OpeningRangeBreakoutIntradayStrategy", "VWAPMeanReversionIntradayStrategy", "GapReversalIntradayStrategy",
			"CryptoRangeBreakoutIntradayStrategy", "CryptoVWAPReversionIntradayStrategy"
	);

	public TradingRecord runConfiguredStrategy(BarSeries barSeries, Strategy strategyToRun) {
		double borrowingFee = 0.00001;
		double fee = resolveFee(strategyToRun.getName());
		CostModel transactionCostModel = new LinearTransactionCostModel(fee);
		CostModel borrowingCostModel = new LinearBorrowingCostModel(borrowingFee);
		TradeExecutionModel tradeExecutionModel = CURRENT_CLOSE_STRATEGIES.contains(strategyToRun.getName())
				? new TradeOnCurrentCloseModel()
				: new TradeOnNextOpenModel();
		BarSeriesManager seriesManager = new BarSeriesManager(barSeries, transactionCostModel, borrowingCostModel, tradeExecutionModel);
		if (isBuy) {
			return seriesManager.run(strategyToRun, Trade.TradeType.BUY);
		} else if (isBuyAmount) {
			return seriesManager.run(strategyToRun, Trade.TradeType.BUY, getAmount(barSeries));
		}
		return seriesManager.run(strategyToRun);
	}

	/** Per-trade fee fraction for backtests, by strategy type. */
	private double resolveFee(String strategyName) {
		if (CRYPTO_INTRADAY_STRATEGIES.contains(strategyName)) {
			return cryptoTakerFeePerTradePercent;
		}
		if (INTRADAY_STRATEGIES.contains(strategyName)) {
			return intradayFeePerTradePercent;
		}
		return feePerTradePercent;
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
