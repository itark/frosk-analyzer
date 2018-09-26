package nu.itark.frosk.service;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.TimeSeries;


@RunWith(SpringRunner.class)
@SpringBootTest
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

	
	
}
