package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.ta4j.core.BarSeries;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


//@ExtendWith(SpringExtension.class)
//@Import({IntegrationTestConfiguration.class})
@SpringBootTest
public class TestJTimeSeriesService extends BaseIntegrationTest {

	@Autowired
	private BarSeriesService ts;

	
	@Test
	public void testSecurityId() throws Exception {
		Long sec_id = ts.getSecurityId("SAND.ST");
		assertNotNull(sec_id);
		
	}	
	
	@Test
	public void testGetDataSet() throws Exception {
		List<BarSeries> sec = ts.getDataSet();
		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}		

	
	@Test
	public void testGetDataSetSecurity() throws Exception {
		Long sec_id = ts.getSecurityId("ERIC-B.ST");
		System.out.println("sec_id="+sec_id);
		BarSeries sec = ts.getDataSet(sec_id );
//		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}	
	
	@Test
	public void testGetCandlesFromCoinbase() {

	BarSeries timeSeries = ts.getDataSetFromCoinbase("BTC-EUR");

	System.out.println("barCount="+timeSeries.getBarCount());

	assertNotNull(timeSeries);


	}
	
	@Test
	public void testDatessss(){
		
		long time = 1550664480;  //GMT: Wednesday 20 February 2019 12:08:00 
		
		long m = System.currentTimeMillis();
		
		System.out.println("m="+m);
		
		
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1550664480),ZoneId.systemDefault());

		System.out.println("dateTime="+dateTime);

		
		LocalDate date =
			    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
		
		
		System.out.println("date="+date);
		

		LocalDateTime date2 =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());		
		
		
		System.out.println("date2="+date2);
		
		LocalDateTime date3 =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(m), ZoneId.systemDefault());	
		
		System.out.println("date3="+date3);
		
		
		
	}
	
	
	
	
}
