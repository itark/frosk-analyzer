package nu.itark.frosk.service;

import static org.junit.Assert.assertNotNull;

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
	
	@Test
	public void testGOOG() throws Exception {
		TimeSeries sec = ts.getDataSet("GOOG");
		assertNotNull(sec);
		
		System.out.println("size="+sec.getBarCount());
		
	}	
	
	
	
}
