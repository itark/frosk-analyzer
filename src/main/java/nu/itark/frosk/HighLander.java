package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;

/**
 * There could be only one...
 * 
 */
@Component
@Slf4j
public class HighLander {

	@Autowired
	DataManager dataManager;
	
	@Autowired
	DataSetRepository dataSetRepository;
	
	@Autowired
	SecurityRepository securityRepository;
	
	@Autowired
	SecurityPriceRepository securityPriceRepository;
	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;

	@Autowired
	TradesRepository tradesRepository;

	@Autowired
	StrategyIndicatorValueRepository strategyIndicatorValueRepository;

	@Autowired
	StrategyAnalysis strategyAnalysis;

	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall(Database database) {
		addDataSetAndSecurities();
		addSecurityPricesFromCoinbase();
		runAllStrategies();
	}

	/**
	 * Full setup, from scratch
	 * Kill them all before.
	 * 
	 */
	public void runCleanInstall(Database database) {
		runClean();
		runInstall(database);
	}
	
	/**
	 * Be aware this will kill them all...
	 * 
	 */
	public void runClean() {
		dataSetRepository.deleteAllInBatch();
		securityRepository.deleteAllInBatch();
		securityPriceRepository.deleteAllInBatch();
		strategyIndicatorValueRepository.deleteAllInBatch();
		tradesRepository.deleteAllInBatch();
		featuredStrategyRepository.deleteAllInBatch();
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
