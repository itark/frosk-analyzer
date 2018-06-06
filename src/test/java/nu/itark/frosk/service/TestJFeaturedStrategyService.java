package nu.itark.frosk.service;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.strategies.RSI2Strategy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJFeaturedStrategyService {
	Logger logger = Logger.getLogger(TestJFeaturedStrategyService.class.getName());
	
	@Autowired
	private FeaturedStrategyService fs;
	


	@Test
	public void testWIKI() {
		List<FeaturedStrategyDTO> fsList = fs.getAllFeaturedStrategies();
		assertNotNull(fsList);
	}

	@Test
	public void testFSE() {
		List<FeaturedStrategyDTO> fsList = fs.getAllFeaturedStrategies();
		assertNotNull(fsList);
	}

	@Test
	public void testFeaturedStrategyRSI_WIKI() {
		List<FeaturedStrategyDTO> fsList = fs.getFeaturedStrategy(RSI2Strategy.class.getSimpleName());
		assertNotNull(fsList);
		
		fsList.forEach(dto -> logger.info("dto="+ReflectionToStringBuilder.toString(dto)));
		
	}	
	
	
	
}
