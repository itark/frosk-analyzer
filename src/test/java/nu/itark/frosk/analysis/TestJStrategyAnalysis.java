package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.logging.Logger;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategyAnalysis {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	
	@Autowired
	private BarSeriesService ts;
	

	@Test
	public final void runSMM() {
		Long sec_id = ts.getSecurityId("BTRST-EUR"); //"BTRST-EUR","BTC-EUR"
		strategyAnalysis.run(SimpleMovingMomentumStrategy.class.getSimpleName(), sec_id);

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



}
