package nu.itark.frosk.analysis;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.service.TimeSeriesService;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJStrategyAnalysis {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	
	@Autowired
	private TimeSeriesService ts;
	

	@Test
	public final void runMM() {
		logger.info("MM="+MovingMomentumStrategy.class.getSimpleName());
		 strategyAnalysis.run(MovingMomentumStrategy.class.getSimpleName(), Long.valueOf(2418800));

//		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}	
	
	@Test
	public final void runRSI2() {
		logger.info("RSI2="+RSI2Strategy.class.getSimpleName());
//		Long sec_id = ts.getSecurityId("ERIC-B.ST");
//		Long sec_id = ts.getSecurityId("SSAB-B.ST");
//		Long sec_id = ts.getSecurityId("ATCO-B.ST");
		Long sec_id = ts.getSecurityId("VOLV-B.ST");
		System.out.println("sec_id="+sec_id);
		strategyAnalysis.run(RSI2Strategy.class.getSimpleName(), sec_id);

//		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}		

	@Test
	public final void runAll() {
		logger.info("All");
		strategyAnalysis.run(null, null);

//		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}		
	
	@Test
	public final void testDateOutput(){
		Date latestTradeDate;
//		latestTradeDate = Date.from(barEntry.getEndTime().toInstant());
		latestTradeDate = new Date();
		
		logger.info("latestTradeDate="+latestTradeDate);
		
	} 
	
	
}
