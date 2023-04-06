package nu.itark.frosk.dataset;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.threeten.bp.LocalDate;

import com.jimmoores.quandl.TabularResult;

import nu.itark.frosk.controller.WebController;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

//@Deprecated  //WIKI databas is invalid, see mail
@Service("wikiTimeSeriesManager")
public class WIKIDataManager {
	Logger logger = Logger.getLogger(WebController.class.getName());

	@Value("${frosk.download.years}")
	public int years;	

	@Autowired
	SecurityPriceRepository securityPriceRepository;		

	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
	DataSetHelper dataSetHelper;		
	
	public void syncronize() {
		logger.info("sync="+Database.WIKI.toString());
		Iterable<Security> securities = securityRepository.findByDatabaseAndActive(Database.WIKI.toString(), true);
		
		securities.forEach(sec -> logger.info("NAME="+ sec.getName()));
		List<SecurityPrice> spList;
		try {
			spList = getDataSet(securities);
		} catch (IOException e) {
			logger.severe("Could not retrieve dataset");
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			securityPriceRepository.save(sp);
		});

	}	
	
	private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException  {
		List<SecurityPrice> sp = new ArrayList<>();

		securities.forEach((security) -> {
			SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());		
			Date fromDate = topSp.getTimestamp();	
			TabularResult tabularResult = QuandlSessionHelper.getTabularResultWithoutApiKey(security, fromDate);

			tabularResult.forEach(row -> {
				SecurityPrice securityPrice = null;
				LocalDate date310 = row.getLocalDate("Date");
				String dateString310 = date310 + " 00:00:00.0";
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
				ZonedDateTime dateTime = ZonedDateTime.parse(dateString310, formatter.withZone(ZoneId.systemDefault()));
				Date date = Date.from(Instant.from(dateTime));

				BigDecimal open = new BigDecimal(row.getDouble("Open"));
				BigDecimal high = new BigDecimal(row.getDouble("High"));
				BigDecimal low = new BigDecimal(row.getDouble("Low"));
				BigDecimal close = new BigDecimal(row.getDouble("Close"));
				Long volume = Double.valueOf(row.getDouble("Volume")).longValue();

				if (date != null && open != null && high != null && low != null && close != null && volume != null) {
					securityPrice = new SecurityPrice(security.getId(), date, open, high, low, close, volume);
					sp.add(securityPrice);
				}
			});

		});

		return sp;
		
	}
	
}
