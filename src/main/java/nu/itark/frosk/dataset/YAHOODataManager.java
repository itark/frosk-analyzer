package nu.itark.frosk.dataset;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
			logger.severe("Could not retrieve dataset");
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			try {
				securityPriceRepository.save(sp);
			} catch (DataIntegrityViolationException e) {
				logger.severe("Duplicate, just go on , for now."+e); //TODO fix the date problem
			}
		});

	}
	
	private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException  {
		logger.info("getDataSet(Iterable<Security> names)");
		List<SecurityPrice> sp = new ArrayList<>();
		Map<String, List<HistoricalQuote>> stockQuotes = getStocks(securities);
		
		stockQuotes.forEach((name,quote) -> {
			logger.info("name="+name);
			try {
				quote.forEach(row -> {
					Date date = Date.from(Instant.ofEpochMilli(row.getDate().getTimeInMillis()));
					SecurityPrice securityPrice = null;
					if (date != null && row.getOpen() != null && row.getHigh() != null && row.getLow() != null
							&& row.getClose() != null && row.getVolume() != null) {
						securityPrice = new SecurityPrice(name, date, row.getOpen(), row.getHigh(), row.getLow(),
								row.getClose(), row.getVolume());
						sp.add(securityPrice);
					} 
				});
				
			} catch (Throwable e) {
				logger.info("Could not get data from Yahoo.");
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
	private Map<String, List<HistoricalQuote>> getStocks(Iterable<Security> securities) throws IOException {
		logger.info("getStocks(Iterable<Security> securities");
		Map<String, List<HistoricalQuote>> stocks = new HashMap<String, List<HistoricalQuote>>();
		
		Calendar to = Calendar.getInstance();
		
        securities.forEach((security) -> {
            Calendar from = Calendar.getInstance(TimeZone.getDefault());
        	boolean isToday = false;
            Date toDay = new Date();
        	SecurityPrice topSp = securityPriceRepository.findTopByNameOrderByTimestampDesc(security.getName());		
            if (topSp != null) {
            	Date lastDate = topSp.getTimestamp();
            	logger.info("security="+security.getName()+ ", found lastDate="+lastDate);
            	if (DateUtils.isSameDay(lastDate, toDay)) {
            		logger.info("isToday::lastDate="+lastDate.toString()+", toDay="+toDay.toString());
            		isToday = true;
            	} else {
//            		from = new Calendar.Builder().setCalendarType("iso8601")
//           			     .setDate(lastDate.getYear(), lastDate.getMonth(), lastDate.getDay()).build();
            		from.setTime(lastDate);
            		from.add(Calendar.DATE, 1); 
            		logger.info("Not today, from set to:"+from.getTime().toString());
            	}
            } else {
                from.add(Calendar.YEAR, -years);
            }
 
    		try {
    			if (!isToday) {
    				logger.info("Retrieving history for "+security.getName()+" from "+from.getTime());
//    				String theX = String.valueOf(from.getTimeInMillis() / 1000);
//    				logger.info("theX="+theX);
//    				Stock stock = YahooFinance.get(security.getName(), from, to, Interval.DAILY);
       				Stock stock = YahooFinance.get(security.getName());
       				List<HistoricalQuote> histQuotes = stock.getHistory(from, to, Interval.DAILY);       				
       				
    				stocks.put(security.getName(), histQuotes);
    			} else {
                	logger.info("Today, no action.");
    			}
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
