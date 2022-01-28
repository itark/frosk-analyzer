package nu.itark.frosk.repo;

import nu.itark.frosk.model.ChartValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.logging.Logger;

@SpringBootTest
public class TestJChartValueRepository {
	Logger logger = Logger.getLogger(TestJChartValueRepository.class.getName());

	
	@Autowired
	ChartValueRepository cvRepo;
	

	@Test
	public void testFindBySecurity() {
		logger.info("count="+cvRepo.count());	
		
		List<ChartValue> spList = cvRepo.findBySecurity("SAND.ST");
		
		logger.info("spList="+spList.size());

	}
	
	
}
