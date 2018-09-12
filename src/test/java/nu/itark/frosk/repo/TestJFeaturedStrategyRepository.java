package nu.itark.frosk.repo;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.FeaturedStrategy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJFeaturedStrategyRepository {
	Logger logger = Logger.getLogger(TestJFeaturedStrategyRepository.class.getName());

	
	@Autowired
	FeaturedStrategyRepository fsRepo;
	

	@Test
	public void testFindBySecurity() {
		logger.info("count="+fsRepo.count());	
		
		List<FeaturedStrategy> fsList = fsRepo.findBySecurity("SAND.ST");
		
		logger.info("fsList="+fsList.size());
		
		fsList.forEach(fs -> logger.info("sec="+fs.getSecurity()+", ld="+fs.getLatestTrade()));

	}
	
	
	
	
}
