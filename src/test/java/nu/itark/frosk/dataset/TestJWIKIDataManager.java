package nu.itark.frosk.dataset;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
public class TestJWIKIDataManager {

	@Autowired
	WIKIDataManager tsManager;
	
	@Test
	public void testSyncronize() {
		tsManager.syncronize();

	}
	
	
	

}
