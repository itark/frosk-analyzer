package nu.itark.frosk.analysis;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJStrategyAnalysis {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	

	@Test
	public final void runMM() {
		logger.info("MM="+MovingMomentumStrategy.class.getSimpleName());
		List<FeaturedStrategyDTO> list = strategyAnalysis.run(MovingMomentumStrategy.class.getSimpleName(), Long.valueOf(2418800));

//		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}	
	
	@Test
	public final void runRSI2() {
		logger.info("RSI2="+RSI2Strategy.class.getSimpleName());
		List<FeaturedStrategyDTO> list = strategyAnalysis.run(RSI2Strategy.class.getSimpleName(), Long.valueOf(310929));

//		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}		
	
	@Test
	public final void runAll() {
		logger.info("All");
		List<FeaturedStrategyDTO> list = strategyAnalysis.run(null, null);

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
