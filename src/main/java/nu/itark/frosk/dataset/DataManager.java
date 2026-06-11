package nu.itark.frosk.dataset;

import java.util.logging.Logger;

import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Autowired
	SecurityRepository securityRepository;

	@Value("${frosk.database.only:YAHOO}")
	private String databaseOnly;

	public void addDatasetSecuritiesIntoDatabase(){
		if ("COINBASE".equals(databaseOnly)) {
			dataSetHelper.addDatasetSecuritiesForCoinBase();
		} else {
			dataSetHelper.addDatasetSecuritiesFromCvsFile();
		}
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

	public void updateSecurityMetaData(Database database) {
		if (database.equals(Database.YAHOO)) {
			yahooDataManager.updateSecurityMetaData();
		}
	}

	public void updateSecurityMetaData(Database database, Security security) {
		if (database.equals(Database.YAHOO)) {
			yahooDataManager.updateSecurityMetaData(security);
		}
	}

	public void insertSecurityPricesIntoDatabase(Database database, String security) {
		if (database.equals(Database.YAHOO)) {
			yahooDataManager.syncronize(security);
		} else if (database.equals(Database.COINBASE)) {
			coinbaseDataManager.syncronize(security);
		} else {
			throw new RuntimeException("No database set!");
		}
	}

}
