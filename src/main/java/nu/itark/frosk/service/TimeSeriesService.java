package nu.itark.frosk.service;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;

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
			timeSeries.add(getDataSet(sp.getName()));
		});
		
		return timeSeries;
		
	}	
	
	/**
	 * Return TimesSeries bases on name in Security.
	 * 
	 * @param name in {@linkplain Security}
	 * @return
	 */
	public TimeSeries getDataSet(String name) {
		List<Bar> bars = new ArrayList<>();
		List<SecurityPrice> securityPrices =securityPriceRepository.findByName(name); 
		
		securityPrices.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());		
			
			Bar bar = new BaseBar(dateTime, row.getOpen().toString(), row.getHigh().toString(), row.getLow().toString(), row.getClose().toString(), row.getVolume().toString());
			
			bars.add(bar);
			
		});
		
		return new BaseTimeSeries(name, bars);
		
		
	}	
	
	
	
	
}
