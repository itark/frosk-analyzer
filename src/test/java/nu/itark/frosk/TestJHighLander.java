package nu.itark.frosk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.HighLander;

@RunWith(SpringRunner.class)
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
