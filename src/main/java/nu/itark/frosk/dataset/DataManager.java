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

	@Autowired
	COINBASEDataManager coinbaseDataManager;

	public void addDatasetSecuritiesIntoDatabase(){
		dataSetHelper.addDatasetSecuritiesFromCvsFile();
		dataSetHelper.addDatasetSecuritiesForCoinBase();
	}
	
	/**
	 * Gets prices from {@linkplain Database} into internal database.
	 * 
	 * @param database
	 */
	public void addSecurityPricesIntoDatabase(Database database) {

		if (database.equals(Database.YAHOO)) {
			logger.info("About to run yahooDataManager.syncronize()...");
			yahooDataManager.syncronize();
		}
		
		if (database.equals(Database.WIKI)) {
			wikiDataManager.syncronize();
		}

		if (database.equals(Database.GDAX)) {
			logger.info("About to run gdaxDataManager.syncronize()...");
			gdaxDataManager.syncronize();
		}		

		if (database.equals(Database.BITFINEX)) {
			logger.info("About to run bitfinexDataManager.syncronize()...");
			bitfinexDataManager.syncronize();
		}

		if (database.equals(Database.COINBASE)) {
			logger.info("About to run coinbaseDataManager.syncronize()...");
			coinbaseDataManager.syncronize();
		}


	}
	
	public void insertSecurityPricesIntoDatabase(Database database, String security) {

		if (database.equals(Database.YAHOO)) {
			yahooDataManager.syncronize(security);
		} else if (database.equals(Database.COINBASE))
			coinbaseDataManager.syncronize(security);
		else {
			throw new RuntimeException("No database set!");
		}

	}	
	
	
	
}
