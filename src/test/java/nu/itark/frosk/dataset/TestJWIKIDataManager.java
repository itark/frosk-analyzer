package nu.itark.frosk.dataset;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.TimeSeries;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJWIKIDataManager {

	@Autowired
	WIKIDataManager tsManager;
	
	@Test
	public void testSyncronize() {
		tsManager.syncronize();

	}
	
	
	

}
