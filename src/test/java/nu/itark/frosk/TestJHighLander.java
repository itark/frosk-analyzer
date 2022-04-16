package nu.itark.frosk;

import nu.itark.frosk.dataset.Database;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestJHighLander {

	@Autowired
	HighLander highLander;
	
	@Test
	public void runInstallCoinbase() {
		highLander.runInstall(Database.COINBASE);
		
	}

	@Test
	public void runCleanInstallCoinbase() {
		highLander.runCleanInstall(Database.COINBASE);
		
	}	
	
	@Test
	public void runClean() {
		highLander.runClean();
	}
	
	
}
