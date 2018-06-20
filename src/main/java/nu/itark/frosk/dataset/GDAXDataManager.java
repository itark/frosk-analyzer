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

import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

@Service
public class GDAXDataManager extends TimeSeriesManager {
	Logger logger = Logger.getLogger(GDAXDataManager.class.getName());

	@Value("${frosk.download.years}")
	public int years;	

	@Autowired
	SecurityPriceRepository securityPriceRepository;		

	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
	DataSetHelper dataSetHelper;		
	
	public void syncronize() {
		logger.info("syncronize database="+Database.GDAX.toString());
		Iterable<Security> securities = securityRepository.findByDatabase(Database.GDAX.toString()); 
		
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
			TabularResult tabularResult = QuandlSessionHelper.getTabularResultWithoutApiKey(security);
	
			
			logger.info("tabularResult="+tabularResult.toPrettyPrintedString());
			
			
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
				BigDecimal close = new BigDecimal(row.getDouble("Open"));  //TODO valid to use open
				Long volume = Double.valueOf(row.getDouble("Volume")).longValue();

				if (date != null && !open.equals(BigDecimal.ZERO) && !high.equals(BigDecimal.ZERO) && !low.equals(BigDecimal.ZERO) && !close.equals(BigDecimal.ZERO) && !volume.equals(BigDecimal.ZERO)) {
					securityPrice = new SecurityPrice(security.getName(), date, open, high, low, close, volume);
					sp.add(securityPrice);
				}
			});
			
		});
		
		return sp;
		
	}
	
	
	
	
	

}
