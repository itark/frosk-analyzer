package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.dataset.COINBASEDataManager;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.*;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJSecurityPriceRepository extends BaseIntegrationTest {

	@Autowired
	SecurityPriceRepository securityPriceRepository;

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	ProductProxy productProxy;

	@Autowired
	COINBASEDataManager coinbaseDataManager;


	@Test
	public void testForDecimalsInTable() throws IOException {
		log.info("hello="+ ReflectionToStringBuilder.toString(securityRepository.findByName("SHPING-EUR")));

		Security security = securityRepository.findByName("SHPING-EUR");

		//logger.info("count="+secRepo.count());
		SecurityPrice sp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
		log.info("sp="+ ReflectionToStringBuilder.toString(sp));
		securityPriceRepository.deleteAllInBatch(Arrays.asList(sp));

/*
		Instant startTime = Instant.now().minus(400, ChronoUnit.MINUTES);
		Instant endTime = Instant.now().minus(100, ChronoUnit.MINUTES);
		Candles candles = productProxy.getCandles("SHPING-EUR", startTime,endTime, Granularity.FIFTEEN_MIN );
*/
		List<Security> securities = Arrays.asList(security);

		Map<Long, List<Candle>> currencyCandlesMap  = coinbaseDataManager.getCandles(securities, Granularity.ONE_DAY);

		currencyCandlesMap.forEach((sec_id, candleList) -> {
			candleList.forEach(row -> {
				Date date = Date.from(row.getTime());
				SecurityPrice securityPrice = null;
				if (date != null && row.getOpen() != null && row.getHigh() != null && row.getLow() != null
						&& row.getClose() != null && row.getVolume() != null) {
					securityPrice = new SecurityPrice(sec_id,
							date,
							row.getOpen(),
							row.getHigh(),
							row.getLow(),
							row.getClose(),
							row.getVolume().longValue());
					//sp.add(securityPrice);
					log.info("\nsecurityPrice:"+ReflectionToStringBuilder.toString(securityPrice));
					//securityPriceRepository.save(sp);

				}
			});
		});

		securityPriceRepository.findBySecurityIdOrderByTimestamp(security.getId()).forEach(o-> {
			System.out.println("Saved SecurityPrice:"+ReflectionToStringBuilder.toString(o));
		});

	}

	
	@Test
	public void testTestMultiples() throws IOException {	
		String[] symbols = new String[] { "INTC", "BABA", "TSLA", "AIR.PA", "YHOO" };
		// Can also be done with explicit from, to and Interval parameters
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL)); //https://github.com/sstrickx/yahoofinance-api/issues/126

		Map<String, Stock> stocks = YahooFinance.get(symbols, true);
		Stock intel = stocks.get("INTC");
		Stock airbus = stocks.get("AIR.PA");
	}
	

	@Test
	public void testfindTopBySecurityIdOrderByTimestampDesc() {
		log.info("count="+securityPriceRepository.count());
		
		SecurityPrice sp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(10202802); //id=10202802,name=SHPING-EUR
		
		
		log.info("sp="+ ReflectionToStringBuilder.toString(sp));

	}

	@Test
	public void testSecurityId() {
		log.info("count="+securityPriceRepository.count());

		SecurityPrice sp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(122113);


		log.info("sp="+sp);

	}

	@Test
	public void testHistory() throws IOException {
		//BROKEN?
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		Stock tesla = YahooFinance.get("TSLA", true);
		System.out.println(tesla.getHistory());
	
	
	}

	@Test
	public void testHistory2() throws IOException {
	
		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
		from.add(Calendar.YEAR, -1); // from 1 year ago

		Stock google = YahooFinance.get("VOLV-B.ST");
		List<HistoricalQuote> googleHistQuotes = google.getHistory(from, to, Interval.DAILY);	
	
	}	
	
	
}
