package nu.itark.frosk.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDataManager {

	@Autowired
	DataManager dataManager;
	
	@Test
	public final void run() {

		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO);
		
	}
	
}
