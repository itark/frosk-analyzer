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
	public final void runYahoo() {

		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);  
		
	}
	
	@Test
	public final void runYahooOneSecurity() {

		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO, "SAND.ST");  
		
	}	
	
	
	@Test
	public final void runWiki() {

		dataManager.addSecurityPricesIntoDatabase(Database.WIKI);  
		
	}	

	@Test
	public final void runGdax() {

		dataManager.addSecurityPricesIntoDatabase(Database.GDAX);  
		
	}		
	
	@Test
	public final void runBitfinex() {

		dataManager.addSecurityPricesIntoDatabase(Database.BITFINEX);  
		
	}	
	
	
	@Test
	public final void runSecuritiesIntoDatabase() {

		dataManager.addDatasetSecuritiesIntoDatabase();
		
	}
	
	
}
