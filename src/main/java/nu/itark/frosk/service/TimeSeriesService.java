package nu.itark.frosk.service;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.num.PrecisionNum;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

@Service
public class TimeSeriesService  {
	Logger logger = Logger.getLogger(TimeSeriesService.class.getName());
	
	@Autowired
	SecurityPriceRepository securityPriceRepository;	
	
	@Autowired
	SecurityRepository securityRepository;	
	
	/**
	 * Retrive from {@linkplain SecurityPriceRepository}
	 * 
	 * @return
	 */
	public List<TimeSeries> getDataSet() {
		Iterable<Security> spList = securityRepository.findAll();  
		List<TimeSeries> timeSeries = new ArrayList<TimeSeries>();
		
		spList.forEach(sp -> {
			timeSeries.add(getDataSet( getSecurityId(sp.getName())  ));
		});
		
		return timeSeries;
		
	}	

	
	Long getSecurityId(String securityName) {
		return securityRepository.findByName(securityName).getId();
	}
	
	/**
	 * Return TimesSeries bases on name in Security.
	 * 
	 * @param name in {@linkplain Security}
	 * @return TimeSeries
	 */
	public TimeSeries getDataSet(String securityName) {
		return  getDataSet( getSecurityId(securityName)  );
	}
	
	
	/**
	 * Return TimesSeries bases on id in Security.
	 * 
	 * @param security_id in {@linkplain Security}
	 * @return TimeSeries
	 */
	public TimeSeries getDataSet(Long security_id) {
		Security security = securityRepository.findById(security_id);
		//Sanity check
		if (security == null){
			throw new RuntimeException("Security is null");
		}

		TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName(security.getName()).withNumTypeOf(PrecisionNum.class).build();
		List<SecurityPrice> securityPrices =securityPriceRepository.findBySecurityId(security.getId()); 
		
		securityPrices.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());		
		     series.addBar(dateTime, row.getOpen(), row.getHigh(),  row.getLow(), row.getClose(), row.getVolume());			
		});
		
		return series;
		
	}	
	
	
}
