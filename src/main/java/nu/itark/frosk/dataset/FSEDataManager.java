package nu.itark.frosk.dataset;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;

import com.jimmoores.quandl.Row;
import com.jimmoores.quandl.SessionOptions;
import com.jimmoores.quandl.TabularResult;
import com.jimmoores.quandl.classic.ClassicQuandlSession;

import nu.itark.frosk.controller.WelcomeController;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;

@Service("fseTimeSeriesManager")
public class FSEDataManager extends TimeSeriesManager {
	Logger logger = Logger.getLogger(WelcomeController.class.getName());
	private final String datasetCodes = "FSE-datasets-codes-manual.csv";  
//	private final String datasetCodes = "FSE-datasets-codes.csv";  
	

	@Autowired
	SecurityPriceRepository securityRepository;			
	
	public void syncronize() {
		List<SecurityCode> codes = SecurityCode.loadCvsSeries(datasetCodes);
		logger.info("codes=" + codes);
		List<SecurityPrice> spList;
		try {
			spList = getDataSet(codes);
		} catch (IOException e) {
			logger.severe("Could not retrieve dataset from cvs-file:"+datasetCodes);
			throw new RuntimeException(e);
		}

		spList.forEach((sp) -> {
			securityRepository.save(sp);
		});
		
		logger.info("Saved securities in "+datasetCodes+" into database.");

	}

	private List<SecurityPrice> getDataSet(List<SecurityCode> codes) throws IOException  {
		List<SecurityPrice> sp = new ArrayList<>();
	
		codes.forEach((code) -> {
			TabularResult tabularResult = QuandlSessionHelper.getTabularResultWithoutApiKey(code);
	
			tabularResult.forEach(row -> {
				SecurityPrice security = null;
				LocalDate date = LocalDate.from(Instant.ofEpochMilli(row.getLocalDate("Date").toEpochDay()));				
				Date date2 = Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
				BigDecimal open = new BigDecimal(row.getDouble("Open"));
				BigDecimal high = new BigDecimal(row.getDouble("High"));
				BigDecimal low = new BigDecimal(row.getDouble("Low"));
				BigDecimal close = new BigDecimal(row.getDouble("Close"));
				Long volume = Double.valueOf(row.getDouble("Traded Volume")).longValue();

				if (date != null && open != null && high != null && low != null
						&& close != null && volume != null) {
					security = new SecurityPrice(code.getCode(), date2, open, high, low, close, volume);
					sp.add(security);
				}
			});
			
		});
		
		return sp;
		
	}
	
	
	
	
	

}
