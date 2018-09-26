package nu.itark.frosk.dataset;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

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
				logger.severe("Duplicate ."+e);
				throw e;
			}
		});

	}
	
	/**
	 * Download prices and insert into database for one security
	 */
	public void syncronize(String sec) {
		logger.info("sync="+Database.YAHOO.toString());
		Security security = securityRepository.findByName(sec);
		List<Security> securities = Arrays.asList(security);
		
		securities.forEach(sc -> logger.info("NAME="+ sc.getName()));
		
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
				logger.severe("Duplicate ." + e);
				throw e;
			}
		});

	}	
	
	
	
	
	private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException  {
		logger.info("getDataSet(Iterable<Security> names)");
		List<SecurityPrice> sp = new ArrayList<>();
		Map<Long, List<HistoricalQuote>> stockQuotes = getStocks(securities);
		
		stockQuotes.forEach((sec_id,quote) -> {
//			logger.info("sec_id="+sec_id);
			try {
				quote.forEach(row -> {
					Date date = Date.from(Instant.ofEpochMilli(row.getDate().getTimeInMillis()));
					SecurityPrice securityPrice = null;
					if (date != null && row.getOpen() != null && row.getHigh() != null && row.getLow() != null
							&& row.getClose() != null && row.getVolume() != null) {
						securityPrice = new SecurityPrice(sec_id, date, row.getOpen(), row.getHigh(), row.getLow(),
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
	private Map<Long, List<HistoricalQuote>> getStocks(Iterable<Security> securities) throws IOException {
		logger.info("getStocks(Iterable<Security> securities");
		Map<Long, List<HistoricalQuote>> stocks = new HashMap<Long, List<HistoricalQuote>>();
		
		Calendar to = Calendar.getInstance(TimeZone.getDefault());
		
        securities.forEach((security) -> {
            Calendar from = Calendar.getInstance(TimeZone.getDefault());
        	boolean isToday = false;
            Date toDay = new Date();
        	SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());		
            if (topSp != null) {
            	Date lastDate = topSp.getTimestamp();
            	logger.info("security="+security.getName()+ ", found lastDate="+lastDate);
            	if (DateUtils.isSameDay(lastDate, toDay)) {
            		logger.info("isToday::lastDate="+lastDate.toString()+", toDay="+toDay.toString());
            		isToday = true;
            	} else if (DateUtils.isSameDay(lastDate, DateUtils.addDays(toDay, -1))) {
            		logger.info("last is yeasterday");
            		from.setTime(lastDate);
            		from.add(Calendar.DATE, 2); 
            	}
            	else {
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
       				Stock stock = YahooFinance.get(security.getName());
       				List<HistoricalQuote> histQuotes = stock.getHistory(from, to, Interval.DAILY);  
       				
    				stocks.put(security.getId(), histQuotes);
    
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

	
	
}
