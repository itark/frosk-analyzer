package nu.itark.frosk.dataset;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJBITFINEXDataManager {
	Logger logger = Logger.getLogger(TestJBITFINEXDataManager.class.getName());
	
	
	@Autowired
	BITFINEXDataManager tsManager;
	
	@Test
	public void testSyncronize() {
		
		logger.info("number of recordes added="+tsManager.syncronize());

	}
	
}
