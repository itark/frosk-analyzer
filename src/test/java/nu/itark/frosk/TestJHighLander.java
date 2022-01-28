package nu.itark.frosk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestJHighLander {

	@Autowired
	HighLander highLander;
	
	
	@Test
	public void runInstall() {
		highLander.runInstall();
		
	}

	@Test
	public void runCleanInstall() {
		highLander.runCleanInstall();
		
	}	
	
	@Test
	public void runClean() {
		highLander.runClean();
		
	}	
	
	
}
