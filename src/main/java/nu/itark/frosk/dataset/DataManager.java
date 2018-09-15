package nu.itark.frosk.dataset;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for downloading external data into internal database
 * 
 * @author fredrikmoller
 *
 */
@Service
public class DataManager {
	Logger logger = Logger.getLogger(DataManager.class.getName());
	
	@Autowired
	DataSetHelper dataSetHelper;
	
	@Autowired
	YAHOODataManager yahooDataManager;

	@Autowired
	WIKIDataManager wikiDataManager;	
	
	@Autowired
	GDAXDataManager gdaxDataManager;		
	
	@Autowired
	BITFINEXDataManager bitfinexDataManager;		
	
	/**
	 * Insert securities from all cvs-files.
	 */
	public void insertSecuritiesIntoDatabase(){
		dataSetHelper.insertSecurityFromCvsFile();
	}
	
	public void insertSecurityPricesIntoDatabase(Database database, boolean hasSecurities) {

		if (hasSecurities && database.equals(Database.YAHOO)) {
			yahooDataManager.syncronize();
		}
		
		if (hasSecurities && database.equals(Database.WIKI)) {
			wikiDataManager.syncronize();
		}

		if (hasSecurities && database.equals(Database.GDAX)) {
			logger.info("About to run gdaxDataManager.syncronize()...");
			gdaxDataManager.syncronize();
		}		

		if (hasSecurities && database.equals(Database.BITFINEX)) {
			logger.info("About to run bitfinexDataManager.syncronize()...");
			bitfinexDataManager.syncronize();
		}			
		
		
		
		
	}
	
	public void insertSecurityPricesIntoDatabase(Database database, String security, boolean hasSecurities) {

		if (hasSecurities && database.equals(Database.YAHOO)) {
			yahooDataManager.syncronize(security);
		} else {
			throw new RuntimeException("No database set!");
		}
		
		
	}	
	
	
	
}
