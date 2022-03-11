package nu.itark.frosk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
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
	
	@Autowired
	FeaturedStrategyRepository strategyRepo;
	
	@Autowired
	StrategyAnalysis strategyAnalysis;
	
	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall() {
		addDataSetAndSecurities();
		//addSecurityPricesFromYahoo();
		addSecurityPricesFromCoinbase();
		runAllStrategies();
		
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
		
		runInstall();
		
	}	
	
	/**
	 * Be aware this will kill them all...
	 * 
	 */
	public void runClean() {
		dataSetRepo.deleteAllInBatch();
		securityRepo.deleteAllInBatch();
		securityPriceRepo.deleteAllInBatch();
		strategyRepo.deleteAllInBatch();
		
	}		
	
	
	/**
	 * Insert or add DataSet and its securities defined in csv-file.
	 * 
	 */
	private void addDataSetAndSecurities() {
		dataManager.addDatasetSecuritiesIntoDatabase();
	}
	
	private void addSecurityPricesFromYahoo() {
		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);
	}

	private void addSecurityPricesFromCoinbase() {
		dataManager.addSecurityPricesIntoDatabase(Database.COINBASE);
	}

	/**
	 * Run all stratregies on all securities.
	 * This will insert result into {@linkplain FeaturedStrategyRepository}
	 * 
	 */
	private void runAllStrategies() {
		strategyAnalysis.run(null, null);

	}

	
}
