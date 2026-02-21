package nu.itark.frosk.dataset;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.RecommendationTrendRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.SecurityPriceRepository;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockStats;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
public class TestJYahooDataManager extends BaseIntegrationTest  {

	@Autowired
	YAHOODataManager yahooDataManager;
	
	@Autowired
	SecurityPriceRepository securityPriceRepository;

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	RecommendationTrendRepository recommendationTrendRepository;
	

	@MockBean
	Coinbase coinbase;

	@MockBean
	ProductService productService;
	
	@Test
	public void syncOne(){
		String securityName = "OPSYH.ST";
		yahooDataManager.syncronize(securityName); //	ALFA.ST, AAK.ST, DOFG.OL, "DOFG.OL", VOLCAR-B.ST, ESSITY-B.ST,BILL.ST,ALLEI.ST,HTRO.ST, OPSYH.ST

		final Security security = securityRepository.findByName(securityName);
		final List<SecurityPrice> bySecurityIdOrderByTimestamp = securityPriceRepository.findBySecurityIdOrderByTimestamp(security.getId());

		final SecurityPrice securityPrice = bySecurityIdOrderByTimestamp.get(bySecurityIdOrderByTimestamp.size()-1);
		log.info("securityPrice:{}",ReflectionToStringBuilder.toString(securityPrice));

	}
	

	@Test
	public void test() throws IOException {

//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
//		Stock stock = YahooFinance.get("VOLV-B.ST",true);
		Stock stock;
		try {
			stock = YahooFinance.get("NDA-SE.ST",true);

			BigDecimal price = stock.getQuote().getPrice();
			BigDecimal change = stock.getQuote().getChangeInPercent();
			BigDecimal peg = stock.getStats().getPeg();
			BigDecimal dividend = stock.getDividend().getAnnualYieldPercent();

			stock.print();	
			
			StockStats xx =stock.getStats();
			
			System.out.println("xx="+ReflectionToStringBuilder.toString(xx));
		
		
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	
	
	}
	
	@Test
	public void test2() throws IOException {
		Stock one = YahooFinance.get("GOOG", true);
		System.out.println("one:"+one.getHistory());

		Stock two = YahooFinance.get("SAND.ST",true);
		System.out.println("two:"+two.getHistory());

	}	
	
	@Test
	public void test3() throws IOException {

		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
		from.add(Calendar.DAY_OF_MONTH, 1); 
		
		System.out.println("from="+from.getTime());
	
		String theX = String.valueOf(from.getTimeInMillis() / 1000);
		log.info("theX="+theX);		
		
		
		Stock google = YahooFinance.get("SAND.ST", from, to, Interval.DAILY);

		System.out.println("google="+google);
		
//		 Stock google = YahooFinance.get("GOOG");
		List<HistoricalQuote> googleHistQuotes = google.getHistory();
		
		System.out.println("googleHistQuotes="+googleHistQuotes);
		
	
		
	}

	@Test
	public void testTestMultiples() throws IOException {	
		String[] symbols = new String[] { "INTC", "BABA", "TSLA", "AIR.PA", "YHOO" };
		// Can also be done with explicit from, to and Interval parameters
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL)); //https://github.com/sstrickx/yahoofinance-api/issues/126
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)); //https://github.com/sstrickx/yahoofinance-api/issues/126
		Map<String, Stock> stocks = YahooFinance.get(symbols, true);
		Stock intel = stocks.get("INTC");
		Stock airbus = stocks.get("AIR.PA");
	}
	


	
	@Test
	public void testSyncronize() {
		yahooDataManager.syncronize();
	 
		log.info("count="+ securityPriceRepository.count());
		
	}	

	
	@Test
	public void testHistory2() throws IOException {
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
//		from.add(Calendar.YEAR, -1); // from 1 year ago
		from.add(Calendar.DATE, -1); 

		Stock volvo = YahooFinance.get("VOLV-B.ST");
		List<HistoricalQuote> quotes = volvo.getHistory(from, to, Interval.DAILY);	
		assertNotNull(quotes);
	
	}	
	
	
//	@Test
//	public void testGetDataSet() throws IOException {
//		yahooDataManager.getDataSet(Arrays.asList(new Security("GOOG", "desc", Database.YAHOO.toString()) ));
//	 
//		logger.info("count="+securityPriceRepository.count());
//		
//	}	
		
	
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
	
	log.info("intelList.size()="+intelList.size());
	
	
	
	Stock airbus = stocks.get("BABA");
	}
	
	@Test
	public void testOfOneDay(){
		Calendar from = Calendar.getInstance();
		log.info("from="+from.getTime());
		from.add(Calendar.DATE, 1); 		
		log.info("from="+from.getTime());
		
	}
	
	@Test
	public void testOfOneYear(){
		Calendar from = Calendar.getInstance();
		log.info("from="+from.getTime());
		from.add(Calendar.YEAR, -1); 		
		log.info("from="+from.getTime());
		
	}

	@Test
	public void testUpdateMetaData(){
		String securityName = "OODA.ST"; //ABB.ST, ESSITY-B.ST, MER.ST, BICO-B.ST, AIK-B.ST, AGTIRA-B.ST, KAV.ST, BRIX, OODA.ST
		final Security security = securityRepository.findByName(securityName);
		log.info("security:{}",security);
		yahooDataManager.updateWithMetaData(security);
		final Security securityUpdate = securityRepository.findByName(securityName);
		log.info("securityUpdate:{}",securityUpdate);

		List<RecommendationTrend> bySecurityOrderByPeriod = recommendationTrendRepository.findBySecurityOrderByPeriod(security);
		log.info("bySecurityOrderByPeriod:{}",bySecurityOrderByPeriod);

	}


}
