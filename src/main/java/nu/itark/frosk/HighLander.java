package nu.itark.frosk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

/**
 * There could be only one...
 * 
 */
@Service
public class HighLander {

	@Autowired
	DataManager dataManager;
	
	@Autowired
	DataSetRepository dataSetRepo;
	
	@Autowired
	SecurityRepository securityRepo;
	
	@Autowired
	SecurityPriceRepository securityPriceRepo;
	
	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall() {
		addDataSetAndSecurities();
		addDataSetAndSecuritiesFromYahoo();
		
	}


	/**
	 * Full setup, from scratch
	 * Kill them all before.
	 * 
	 */
	public void runCleanInstall() {
		dataSetRepo.deleteAllInBatch();
		securityRepo.deleteAllInBatch();
		securityPriceRepo.deleteAllInBatch();
		
		addDataSetAndSecurities();
		addDataSetAndSecuritiesFromYahoo();
		
		
	}	
	
	/**
	 * Insert or add DataSet and its securities defined in csv-file.
	 * 
	 */
	private void addDataSetAndSecurities() {
		
		dataManager.addDatasetSecuritiesIntoDatabase();
		
	}
	
	/**
	 * Add security prices from yahoo, by Securities defined in {@linkplain SecurityRepository}
	 * 
	 */
	private void addDataSetAndSecuritiesFromYahoo() {
		
		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);
		
	}	
	
}
