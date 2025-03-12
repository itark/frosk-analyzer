package nu.itark.frosk.dataset;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class YAHOODataManager  {
//	Logger logger = Logger.getLogger(YAHOODataManager.class.getName());
	
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
		log.info("sync="+Database.YAHOO.toString());
		Iterable<Security> securities = securityRepository.findByDatabaseAndActive(Database.YAHOO.toString(), true);
		
		securities.forEach(sec -> log.info("NAME="+ sec.getName()));
		
		List<SecurityPrice> spList;
		try {
			spList = getDataSet(securities);
		} catch (IOException e) {
			log.error("Could not retrieve dataset");
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			try {
				securityPriceRepository.save(sp);
			} catch (DataIntegrityViolationException e) {
				log.error("Delivered duplicates on sp.getSecurityId(): "+ sp.getSecurityId()+ ", continues....");
				//sort of ok, continue
			}
		});

	}
	
	/**
	 * Download prices and insert into database for one security
	 */
	public void syncronize(String sec) {
		log.info("sync="+Database.YAHOO.toString());
		Security security = securityRepository.findByName(sec);

		Assert.notNull(security, "security can not be null");
		
		List<Security> securities = Arrays.asList(security);
		securities.forEach(sc -> log.info("NAME="+ sc.getName()));
		
		List<SecurityPrice> spList;
		try {
			spList = getDataSet(securities);
		} catch (IOException e) {
			log.error("Could not retrieve dataset");
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			try {
				securityPriceRepository.save(sp);
			} catch (DataIntegrityViolationException e) {
				log.error("Duplicate ." + e);
				//continue
			}
		});

	}	
	
	private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException {
		log.info("getDataSet(Iterable<Security> names)");
		List<SecurityPrice> sp = new ArrayList<>();
		Map<Long, List<HistoricalQuote>> stockQuotes = getStocks(securities);

		if (stockQuotes != null) {

			stockQuotes.forEach((sec_id, quote) -> {
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

			});

		}

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
		log.info("getStocks(Iterable<Security> securities");
		Map<Long, List<HistoricalQuote>> stocks = new HashMap<Long, List<HistoricalQuote>>();

		Calendar to = Calendar.getInstance(TimeZone.getDefault());

		securities.forEach((security) -> {
			Calendar from = Calendar.getInstance(TimeZone.getDefault());
			boolean isToday = false;
			Date toDay = new Date();
			SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
			if (topSp != null) {
				Date lastDate = topSp.getTimestamp();
				log.info("security=" + security.getName() + ", found lastDate=" + lastDate);
				if (DateUtils.isSameDay(lastDate, toDay)) {
					log.info("isToday ::lastDate=" + lastDate.toString() + ", toDay=" + toDay.toString());
					isToday = true;
				} else if (DateUtils.isSameDay(lastDate, DateUtils.addDays(toDay, -1))) {
					log.info("last is yeasterday");
					from.setTime(lastDate);
					from.add(Calendar.DATE, 1);
				} else {
					from.setTime(lastDate);
					from.add(Calendar.DATE, 1);
					log.info("Not today, from set to:" + from.getTime().toString());
				}
			} else {
				from.add(Calendar.YEAR, -years);
			}

			if (!isToday) {
				log.info("Retrieving history for " + security.getName() + " from " + from.getTime());
				Stock stock;
				try {
					stock = YahooFinance.get(security.getName());
					List<HistoricalQuote> histQuotes = stock.getHistory(from, to, Interval.DAILY);
					stocks.put(security.getId(), histQuotes);

				} catch (FileNotFoundException fe) {
					log.info("Not found: "+ fe.getMessage() + " continues...");
					// continue
				} catch (Exception e) {
					log.error("ERROR:", e);
					// throw e;
				}

			} else {
				log.info("Today, no action.");
			}
		});

		return stocks;

	}

	private boolean isFriday(Date date) {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(date);
		if ((c1.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) ) { 
			return true;
		} else {
			return false;
		}

	}
	
	
}
