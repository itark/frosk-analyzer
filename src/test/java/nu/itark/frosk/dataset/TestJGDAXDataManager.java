package nu.itark.frosk.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJGDAXDataManager {

	@Autowired
	GDAXDataManager tsManager;
	
	@Test
	public void testSyncronize() {
		tsManager.syncronize();

	}
	
}
