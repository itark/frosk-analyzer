package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
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

	@Value("${frosk.addsecuritypricesfromyahoo}")
	private boolean addSecuritypricesFromYahoo;

	@Value("${frosk.runallstrategies}")
	private boolean runAllStrategies;

	@Value("${frosk.runbot}")
	private boolean runBot;

	@Value("${frosk.updatehedgeindex}")
	private boolean updateHedgeIndex;

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
	StrategyTradeRepository tradesRepository;

	@Autowired
	StrategyIndicatorValueRepository strategyIndicatorValueRepository;

	@Autowired
	StrategyPerformanceRepository strategyPerformanceRepository;

	@Autowired
	StrategyAnalysis strategyAnalysis;

	@Autowired
	BarSeriesService barSeriesService;

	@Autowired
	HedgeIndexService hedgeIndexService;

	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall(Database database) {
		log.info("addDatasetAndSecurities:{}",addDatasetAndSecurities);
		log.info("addSecuritypricesFromCoinbase:{}",addSecuritypricesFromCoinbase);
		log.info("addSecuritypricesFromYahooo:{}",addSecuritypricesFromYahoo);
		log.info("runAllStrategies:{}",runAllStrategies);
		log.info("runBot:{}",runBot);
		log.info("runHedgeIndexStrategies:{}",updateHedgeIndex);

		if (addDatasetAndSecurities) {
			addDataSetAndSecurities();
		}
		if (addSecuritypricesFromCoinbase) {
			addSecurityPricesFromCoinbase();
		}
		if (addSecuritypricesFromYahoo) {
			addSecurityPricesFromYahoo();
		}
		if (runAllStrategies) {
			runAllStrategies();
		}
		//runChooseBestStrategy();
		if (updateHedgeIndex) {
			strategyAnalysis.runHedgeIndexStrategies();
		}
		if (runBot) {
			strategyAnalysis.runningPositions();
		}
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

	public void addSecurityPriceFromCoinbase(String security) {
		dataManager.insertSecurityPricesIntoDatabase(Database.COINBASE, security);
	}

	/**
	 * Run all stratregies on all securities.
	 * This will insert result into {@linkplain FeaturedStrategyRepository}
	 * 
	 */
	private void runAllStrategies() {
		strategyAnalysis.run(null, null);
	}

	public void runStrategy(String strategy, String security) {
		strategyAnalysis.run(strategy, barSeriesService.getSecurityId(security));
	}

	private void runChooseBestStrategy() {
		strategyAnalysis.runChooseBestStrategy();
	}

	public enum ACTION {
		LOAD_DATA,
		RUN_STRATEGY
	}

}
