package nu.itark.frosk.repo;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.ChartValue;

@RunWith(SpringRunner.class)
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
