package nu.itark.frosk.service;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.coinbase.exchange.model.Candle;
import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.ta4j.core.num.DoubleNum;

@Component
public class BarSeriesService  {
	Logger logger = Logger.getLogger(BarSeriesService.class.getName());
	
	@Autowired
	SecurityPriceRepository securityPriceRepository;	
	
	@Autowired
	SecurityRepository securityRepository;	
	
	@Autowired
	ProductProxy productProxy;

	/**
	 * Retrive from {@linkplain SecurityPriceRepository}
	 * 
	 * @return List<BarSeries> for alla securities in database
	 */
	public List<BarSeries> getDataSet() {
		Iterable<Security> spList = securityRepository.findAll();  
		List<BarSeries> BarSeries = new ArrayList<BarSeries>();
		
		spList.forEach(sp -> {
			BarSeries.add(getDataSet( getSecurityId(sp.getName())  ));
		});
		
		return BarSeries;
		
	}	

	
	public Long getSecurityId(String securityName) {
		return securityRepository.findByName(securityName).getId();
	}
	
	/**
	 * Return TimesSeries bases on name in Security.
	 * 
	 * @param  {@linkplain Security}
	 * @return BarSeries
	 */
	public BarSeries getDataSet(String securityName) {
		return  getDataSet( getSecurityId(securityName)  );
	}
	
	/**
	 * Return TimesSeries bases on id in Security.
	 * 
	 * @param security_id in {@linkplain Security}
	 * @return BarSeries
	 */
	public BarSeries getDataSet(Long security_id) {
		Optional<Security> security = securityRepository.findById(security_id);
		//Sanity check
		if (security == null){
			throw new RuntimeException("Security is null");
		}

		BarSeries series = new BaseBarSeriesBuilder().withName(security.get().getName()).withNumTypeOf(DoubleNum.class).build();
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
	 * @return BarSeries
	 *
	 * OBS endast 10 dagar för gammalt data
	 *
	 */
	public BarSeries getDataSetFromCoinbase(String productId) {
		BarSeries series = new BaseBarSeriesBuilder().withName(productId).withNumTypeOf(DecimalNum.class).build();

		//TODO ser över tiden
		Instant startTime = Instant.now().minus(300, ChronoUnit.DAYS);
		Instant endTime = startTime.plus(10, ChronoUnit.DAYS);
	//	Instant endTime = Instant.now();

		System.out.println("startTime:"+startTime);
		System.out.println("endTime"+endTime);

  		Candles candles = productProxy.getCandles(productId, startTime,endTime, Granularity.ONE_DAY );

		List<Candle> sortedList = candles.getCandleList()
				.stream()
				.sorted((p1, p2)-> p1.getTime().compareTo(p2.getTime()))
				.collect(Collectors.toList());

		sortedList.forEach(row -> {
			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTime(),ZoneId.systemDefault());
			series.addBar(dateTime, row.getOpen(), row.getHigh(), row.getLow(), row.getClose(), row.getVolume());
		});

		return series;

	}

	
}
