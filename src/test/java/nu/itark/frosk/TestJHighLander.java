package nu.itark.frosk;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.Database;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJHighLander extends BaseIntegrationTest {

	@Autowired
	HighLander highLander;
	
	@Test
	public void runInstall() {
		highLander.runInstall(Database.YAHOO);
		
	}

	@Test
	public void runCleanInstall() {
		highLander.runCleanInstall(Database.YAHOO);
		
	}	
	

}
