package nu.itark.frosk.repo;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.model.SecurityPrice;
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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SpringBootTest(classes = FroskApplication.class)
public class TestJSecurityPriceRepository {
	Logger logger = Logger.getLogger(TestJSecurityPriceRepository.class.getName());

	@Autowired
	SecurityPriceRepository secRepo;
	
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
		logger.info("count="+secRepo.count());	
		
		SecurityPrice sp = secRepo.findTopBySecurityIdOrderByTimestampDesc(122113);
		
		
		logger.info("sp="+sp);

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
