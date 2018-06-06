package nu.itark.frosk.analysis;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.strategies.RSI2Strategy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJStrategyAnalysis {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	
	@Test
	public final void runAll() {
		List<FeaturedStrategyDTO> list = strategyAnalysis.runStrategyMatrix();

		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}

	@Test
	public final void runRSI() {
		List<FeaturedStrategyDTO> list = strategyAnalysis.runStrategyMatrix();

		list.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}	
	
	
	
}
