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

		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO, true);  //true if securities in place.ß
		
	}
	
	@Test
	public final void runYahooOneSecurity() {

		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO, "SAND.ST", true);  
		
	}	
	
	
	@Test
	public final void runWiki() {

		dataManager.insertSecurityPricesIntoDatabase(Database.WIKI, true);  //true if securities in place.ß
		
	}	

	@Test
	public final void runGdax() {

		dataManager.insertSecurityPricesIntoDatabase(Database.GDAX, true);  //true if securities in place.ß
		
	}		
	
	@Test
	public final void runBitfinex() {

		dataManager.insertSecurityPricesIntoDatabase(Database.BITFINEX, true);  //true if securities in place.ß
		
	}	
	
	
	@Test
	public final void runSecuritiesIntoDatabase() {

		dataManager.insertDatasetSecuritiesIntoDatabase();
		
	}
	
	
	
	
	
}
