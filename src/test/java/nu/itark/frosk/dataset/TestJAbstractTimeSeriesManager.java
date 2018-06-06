package nu.itark.frosk.dataset;

import org.junit.Before;
import org.junit.Test;

public class TestJAbstractTimeSeriesManager {

	TimeSeriesManager tsManager = null;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@Test
	public void testMaxIndexes() {
		
		long days = TimeSeriesManager.getNumberOfWeekDays();
		System.out.println("days="+days);

		
	}
	
}
