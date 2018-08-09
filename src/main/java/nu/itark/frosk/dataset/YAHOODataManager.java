package nu.itark.frosk.dataset;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * This class is responsible for retrieving and parsing data from yahoo finance.
 * 
 * And save to SecurityPricesRespsitory
 * 
 * Depended lib, see : https://financequotes-api.com
 * 
 * @author fredrikmoller
 *
 */

@Service
public class YAHOODataManager  {
	Logger logger = Logger.getLogger(YAHOODataManager.class.getName());
	
	@Value("${frosk.download.years}")
	public int years;	

	@Autowired
	SecurityPriceRepository securityPriceRepository;		

	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
	DataSetHelper dataSetHelper;
	
	/**
	 * Download prices and insert into database.
	 */
	public void syncronize() {
		logger.info("sync="+Database.YAHOO.toString());
		Iterable<Security> securities = securityRepository.findByDatabase(Database.YAHOO.toString()); 
		
		securities.forEach(sec -> logger.info("NAME="+ sec.getName()));
		
		List<SecurityPrice> spList;
		try {
			spList = getDataSet(securities);
		} catch (IOException e) {
			logger.info("Could not retrieve dataset");
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			securityPriceRepository.save(sp);
		});
		

	}
	
	private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException  {
		logger.info("getDataSet(Iterable<Security> names)");
		List<SecurityPrice> sp = new ArrayList<>();
		Map<String, Stock> stocks = getStocks(securities);

		securities.forEach((security) -> {
			Stock stock = stocks.get(security.getName());
			List<HistoricalQuote> quotes;
			try {
				quotes = stock.getHistory();
				quotes.forEach(row -> {
					Date date = Date.from(Instant.ofEpochMilli(row.getDate().getTimeInMillis()));
					SecurityPrice securityPrice = null;
					if (date != null && row.getOpen() != null && row.getHigh() != null && row.getLow() != null
							&& row.getClose() != null && row.getVolume() != null) {
						securityPrice = new SecurityPrice(security.getName(), date, row.getOpen(), row.getHigh(), row.getLow(),
								row.getClose(), row.getVolume());
						sp.add(securityPrice);
					} 
				});
				
			} catch (Throwable e) {
				logger.info("Could not get data from Yahoo.");
				e.printStackTrace();
				throw new RuntimeException(e);
			}


		});

		return sp;
		
	}
	
	
	/**
	 * Get DAILY Stocks on provided Securities.
	 * 
	 * @param securities
	 * @return Map<String, Stock>
	 * @throws IOException
	 */
	private Map<String, Stock> getStocks(Iterable<Security> securities) throws IOException {
		logger.info("getStocks(Iterable<Security> securities");
		Map<String, Stock> stocks = new HashMap<String, Stock>();
		
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        
        securities.forEach((security) -> {
            SecurityPrice topSp = securityPriceRepository.findTopByNameOrderByTimestampDesc(security.getName());		
            if (topSp != null) {
            	Date fromDate = topSp.getTimestamp();
            	logger.info("security="+security.getName()+ ", found fromDate="+fromDate);
            	from.setTime(fromDate);
            	from.add(Calendar.DATE, 2);  //Bugg in api , increase with 2
            } else {
                from.add(Calendar.YEAR, - years);
            }
            if (from.after(new Date())){
            	logger.info("Today, no action.");
            	return;
            }
            
    		try {
    			logger.info("Retrieving history for "+security.getName()+" from "+from.getTime());
    			Stock stock = YahooFinance.get(security.getName(), from, to, Interval.DAILY);
				stocks.put(security.getName(), stock);
			} catch (IOException e) {
				logger.severe("Could not extract data for security="+security);
				e.printStackTrace();
			} 
        	
        });
        
        return stocks;
        
	}

	/**
	 * Get DAILY Stocks on provided Securities.
	 * 
	 * @param securities
	 * @return Map<String, Stock>
	 * @throws IOException
	 * @Deprecated
	 */
	private Map<String, Stock> getStocks_OBSOLETE(Iterable<Security> securities) throws IOException {
		logger.info("getStocks(Iterable<Security> securities");
		List<String> names = StreamSupport.stream(securities.spliterator(), false)
					.map(Security::getName)
					.collect(Collectors.toList());
        
        String[] symbols = names.stream().toArray(String[]::new);
        String firstSymbolInList = symbols[0];
        
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
 
        SecurityPrice topSp = securityPriceRepository.findTopByNameOrderByTimestampDesc(firstSymbolInList);		
        if (topSp != null) {
        	Date fromDate = topSp.getTimestamp();
        	logger.info("security="+firstSymbolInList+ ", fromDate="+fromDate);
        	from.setTime(fromDate);
        	from.add(Calendar.DATE, 1);
        } else {
            from.add(Calendar.YEAR, - years);
        }
        logger.info("from="+from);
        
        if (from.after(new Date())){
        	logger.info("Today, no action.");
        	return null;
        }
		
//	    CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL)); //https://github.com/sstrickx/yahoofinance-api/issues/126
//		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
		return YahooFinance.get(symbols, from, to, Interval.DAILY); // single request
		
	}
	
	
	
	
	
	
}
