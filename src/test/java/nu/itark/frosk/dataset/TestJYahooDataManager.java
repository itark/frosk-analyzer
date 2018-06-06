package nu.itark.frosk.dataset;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityPriceRepository;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJYahooDataManager {
	Logger logger = Logger.getLogger(TestJYahooDataManager.class.getName());
	
	@Autowired
	YAHOODataManager tsManager;
	
	@Autowired
	SecurityPriceRepository secRepo;
	
	
	@Test
	public void test() throws IOException {
	
		Stock stock = YahooFinance.get("VOLV-B.ST");

		BigDecimal price = stock.getQuote().getPrice();
		BigDecimal change = stock.getQuote().getChangeInPercent();
		BigDecimal peg = stock.getStats().getPeg();
		BigDecimal dividend = stock.getDividend().getAnnualYieldPercent();

		stock.print();	
	
	
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
	public void testSyncronize() {
		tsManager.syncronize();
	 
		logger.info("count="+secRepo.count());
		
	}	

	
	@Test
	public void testHistory2() throws IOException {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
		from.add(Calendar.YEAR, -1); // from 1 year ago

		Stock google = YahooFinance.get("VOLV-B.ST");
		List<HistoricalQuote> googleHistQuotes = google.getHistory(from, to, Interval.DAILY);	
	
	}	
	
	
//	@Test
//	public void testGetDataSet() throws IOException {
//		tsManager.getDataSet(Arrays.asList(new Security("GOOG", "desc", Database.YAHOO.toString()) ));
//	 
//		logger.info("count="+secRepo.count());
//		
//	}	
		
	
	
	@Test
	public void testHistory() throws IOException {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		Stock tesla = YahooFinance.get("TSLA", true);
		System.out.println(tesla.getHistory());
	
	
	}

	@Test
	public void testMultistock() throws IOException {

	CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	Calendar from = Calendar.getInstance();
	Calendar to = Calendar.getInstance();
	from.add(Calendar.YEAR, -1); // from 1 year ago		
		
	String[] symbols = new String[] {"INTC", "BABA", "TSLA"};
	Map<String, Stock> stocks = YahooFinance.get(symbols, from, to); // single request
	Stock intel = stocks.get("INTC");
	
	
	List<HistoricalQuote> intelList =intel.getHistory();
	
	logger.info("intelList.size()="+intelList.size());
	
	
	
	Stock airbus = stocks.get("BABA");
	}
	
}
