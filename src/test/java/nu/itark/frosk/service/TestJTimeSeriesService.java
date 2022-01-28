package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.TimeSeries;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@Slf4j
public class TestJTimeSeriesService {

	@Autowired
	private TimeSeriesService ts;

	
//	@Test
//	public void testGetDataSetPerSecurity() throws Exception {
//		TimeSeries sec = ts.getDataSet("SAND.ST");
//		assertNotNull(sec);
//		
//		System.out.println("size="+sec.getBarCount());
//		
//	}	
	
	@Test
	public void testSecurityId() throws Exception {
		Long sec_id = ts.getSecurityId("SAND.ST");
		assertNotNull(sec_id);
		
	}	
	
	
	
	@Test
	public void testGetDataSet() throws Exception {
		List<TimeSeries> sec = ts.getDataSet();
		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}		

	
	@Test
	public void testGetDataSetSecurity() throws Exception {
		Long sec_id = ts.getSecurityId("ERIC-B.ST");
		System.out.println("sec_id="+sec_id);
		TimeSeries sec = ts.getDataSet(sec_id );
//		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}	
	
//	@Test
//	public void testGetCandlesFromCoinbase() {
//
//	TimeSeries timeSeries = ts.getDataSetFromCoinbase("BTC-EUR");
//
//	log.info("barCount="+timeSeries.getBarCount());
//
//	assertNotNull(timeSeries);
//
//
//	}
	
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
