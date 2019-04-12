package nu.itark.frosk.service;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.num.PrecisionNum;

import nu.itark.frosk.coinbase.exchange.api.marketdata.HistoricRate;
import nu.itark.frosk.crypto.coinbase.MarketDataProxy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.util.DateTimeManager;

@Service
public class TimeSeriesService  {
	Logger logger = Logger.getLogger(TimeSeriesService.class.getName());
	
	@Autowired
	SecurityPriceRepository securityPriceRepository;	
	
	@Autowired
	SecurityRepository securityRepository;	
	
	@Autowired
	MarketDataProxy marketDataProxyCoinbase;
	
	/**
	 * Retrive from {@linkplain SecurityPriceRepository}
	 * 
	 * @return List<TimeSeries> for alla securities in database
	 */
	public List<TimeSeries> getDataSet() {
		Iterable<Security> spList = securityRepository.findAll();  
		List<TimeSeries> timeSeries = new ArrayList<TimeSeries>();
		
		spList.forEach(sp -> {
			timeSeries.add(getDataSet( getSecurityId(sp.getName())  ));
		});
		
		return timeSeries;
		
	}	

	
	public Long getSecurityId(String securityName) {
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
		Optional<Security> security = securityRepository.findById(security_id);
		//Sanity check
		if (security == null){
			throw new RuntimeException("Security is null");
		}

		TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName(security.get().getName()).withNumTypeOf(PrecisionNum.class).build();
		List<SecurityPrice> securityPrices =securityPriceRepository.findBySecurityIdOrderByTimestamp(security.get().getId()); 
		
		securityPrices.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());		
		     series.addBar(dateTime, row.getOpen(), row.getHigh(),  row.getLow(), row.getClose(), row.getVolume());			
		});
		
		return series;
		
	}	
	

	/**
	 * Return TimesSeries bases on productId in Coinbase.
	 * 
	 * NOTE: start and end hardcoded,granularity set to fifteen minutes
	 * 
	 * @param productId 
	 * @return TimeSeries
	 */
	public TimeSeries getDataSetFromCoinbase(String productId) {
		TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName(productId).withNumTypeOf(PrecisionNum.class).build();
		//TODO externalize range: d
		List<HistoricRate> candlesList= marketDataProxyCoinbase.getMarketDataCandles("BTC-EUR", DateTimeManager.start(1), DateTimeManager.end(), MarketDataProxy.GranularityEnum.FIFTEEN_MINUTES.getValue() );
	
		List<HistoricRate> sortedList = candlesList
				.stream()
				.sorted((p1, p2) -> ((Long)p1.getTime()).compareTo(p2.getTime()))
				.collect(Collectors.toList());
		
		sortedList.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(row.getTime()),ZoneId.systemDefault());
			series.addBar(dateTime, row.getOpen(), row.getHigh(), row.getLow(), row.getClose(), row.getVolume());
		});

		return series;
		
	}	
	
	
}
