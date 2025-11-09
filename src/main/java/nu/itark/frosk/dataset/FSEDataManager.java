package nu.itark.frosk.dataset;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jimmoores.quandl.TabularResult;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

@Service("fseTimeSeriesManager")
public class FSEDataManager extends TimeSeriesManager {

	@Value("${frosk.download.years}")
	public int years;	

	@Autowired
	SecurityPriceRepository securityPriceRepository;		

	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
	DataSetHelper dataSetHelper;		
	
	public void syncronize() {
		logger.info("sync="+Database.FSE.toString());
		Iterable<Security> securities = securityRepository.findByDatabaseAndActive(Database.FSE.toString(), true);
		
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
				LocalDate date = LocalDate.from(Instant.ofEpochMilli(row.getLocalDate("Date").toEpochDay()));				
				Date date2 = Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
				BigDecimal open = new BigDecimal(row.getDouble("Open"));
				BigDecimal high = new BigDecimal(row.getDouble("High"));
				BigDecimal low = new BigDecimal(row.getDouble("Low"));
				BigDecimal close = new BigDecimal(row.getDouble("Close"));
				Long volume = Double.valueOf(row.getDouble("Traded Volume")).longValue();

				if (date != null && open != null && high != null && low != null
						&& close != null && volume != null) {
					securityPrice = new SecurityPrice(security.getId(), date2, open, high, low, close, volume);
					sp.add(securityPrice);
				}
			});
			
		});
		
		return sp;
		
	}
	
	
	
	
	

}
