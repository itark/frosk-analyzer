package nu.itark.frosk.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestJAbstractTimeSeriesManager {

	TimeSeriesManager tsManager = null;
	
	@BeforeEach
	public void setUp() throws Exception {
		
	}

	@Test
	public void testMaxIndexes() {
		
		long days = TimeSeriesManager.getNumberOfWeekDays();
		System.out.println("days="+days);

		
	}
	
}
