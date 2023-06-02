package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${frosk.adddatasetandsecurities}")
	private boolean addDatasetAndSecurities;

	@Value("${frosk.addsecuritypricesfromcoinbase}")
	private boolean addSecuritypricesFromCoinbase;

	@Value("${frosk.runallstrategies}")
	private boolean runAllStrategies;

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
	StrategyPerformanceRepository strategyPerformanceRepository;

	@Autowired
	StrategyAnalysis strategyAnalysis;

	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall(Database database) {
		if (addDatasetAndSecurities) {
			addDataSetAndSecurities();
		}
		if (addSecuritypricesFromCoinbase) {
			addSecurityPricesFromCoinbase();
		}
		if (runAllStrategies) {
			runAllStrategies();
		}
		runChooseBestStrategy();
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
		dataSetRepository.deleteAll();
		securityRepository.deleteAll();
		securityPriceRepository.deleteAll();
		strategyIndicatorValueRepository.deleteAll();
		tradesRepository.deleteAll();
		featuredStrategyRepository.deleteAll();
		strategyPerformanceRepository.deleteAll();
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

	private void runChooseBestStrategy() {
		strategyAnalysis.runChooseBestStrategy();
	}

	
}
