package nu.itark.frosk.analysis;

import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.service.TimeSeriesService;
import nu.itark.frosk.strategies.MovingMomentumStrategy;
import nu.itark.frosk.strategies.RSI2Strategy;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@SpringBootTest
public class TestJStrategyAnalysis {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	
	@Autowired
	private TimeSeriesService ts;
	

	@Test
	public final void runSMM() {
		logger.info("MM="+ SimpleMovingMomentumStrategy.class.getSimpleName());
		Long sec_id = ts.getSecurityId("ERIC-B.ST");
//		Long sec_id = ts.getSecurityId("SSAB-B.ST");
//		Long sec_id = ts.getSecurityId("ATCO-B.ST");
//		Long sec_id = ts.getSecurityId("VOLV-B.ST");
		strategyAnalysis.run(MovingMomentumStrategy.class.getSimpleName(), sec_id);

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
	public final void testOpenTradesOne(){
		final List<StrategyTrade> openTrades = strategyAnalysis.getOpenTrades("RSI2Strategy");
		openTrades.forEach(System.out::println);
	}

	@Test
	public final void testOpenTradesAll(){
		final List<StrategyTrade> openTrades = strategyAnalysis.getOpenTrades();
	}

	@Test
	public final void testOpenTradesAll2(){
		List<String> strategies = StrategiesMap.buildStrategiesMap();
		strategies.forEach(s -> {
			final List<StrategyTrade> openTrades = strategyAnalysis.getOpenTrades(s);
			openTrades.forEach(System.out::println);
		});
	}

}
