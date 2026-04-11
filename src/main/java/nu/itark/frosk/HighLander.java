package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.dataset.YAHOODataManager;

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

	@Value("${frosk.buildportfolio}")
	private boolean buildPortfolio;

	@Value("${frosk.updatehedgeindex}")
	private boolean updateHedgeIndex;

	@Value("${frosk.run.omxs30swing}")
	private boolean runOMXS30Swing;

	@Value("${frosk.run.dagstrategin:false}")
	private boolean runDagstrategin;

	@Value("${frosk.updatesecuritymetadata}")
	private boolean updateSecurityMetaData;

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
	RecommendationTrendRepository recommendationTrendRepository;

	@Autowired
	StrategyAnalysis strategyAnalysis;

	@Autowired
	BarSeriesService barSeriesService;

	@Autowired
	HedgeIndexService hedgeIndexService;

	@Autowired
	PortfolioService portfolioService;

	@Autowired
	YAHOODataManager yahooDataManager;

	/**
	 * Full setup, addition
	 * 
	 */
	public void runInstall(Database database) {
		log.info("addDatasetAndSecurities:{}",addDatasetAndSecurities);
		log.info("addSecuritypricesFromCoinbase:{}",addSecuritypricesFromCoinbase);
		log.info("addSecuritypricesFromYahooo:{}",addSecuritypricesFromYahoo);
		log.info("updateSecurityMetaData:{}",updateSecurityMetaData);
		log.info("runAllStrategies:{}",runAllStrategies);
		log.info("runBot:{}",runBot);
		log.info("runHedgeIndexStrategies:{}",updateHedgeIndex);
		log.info("buildPortfolio:{}",buildPortfolio);
		log.info("runOMXS30Swing:{}",runOMXS30Swing);
		log.info("runDagstrategin:{}",runDagstrategin);

		if (addDatasetAndSecurities) {
			addDataSetAndSecurities();
		}
		if (addSecuritypricesFromCoinbase) {
			addSecurityPricesFromCoinbase();
		}
		if (addSecuritypricesFromYahoo) {
			addSecurityPricesFromYahoo();
		}
		if (updateSecurityMetaData) {
			updateSecurityMetaData();
		}
		if (runAllStrategies) {
			runAllStrategies();
		}
		//runChooseBestStrategy(); //TODO fix index-problem
		if (updateHedgeIndex) {
			strategyAnalysis.runHedgeIndexStrategies();
		}
		if (runBot) {
			strategyAnalysis.runningPositions();
		}
		if (buildPortfolio) {
			portfolioService.build();
			log.info("Portfolio snapshot built.");
		}
		if (runOMXS30Swing) {
			strategyAnalysis.runOMXS30Swing();
		}
		if (runDagstrategin) {
			strategyAnalysis.runDagstrateginStrategies();
		}
	}

	/**
	 * Tier 1 — Daily (MON-FRI after market close).
	 * Syncs OMXS30 constituents + runs HedgeIndex strategies (which syncs macro tickers).
	 */
	public void syncTier1() {
		log.info("syncTier1 started");
		yahooDataManager.syncronizeByDataset("OMX30");
		strategyAnalysis.runHedgeIndexStrategies();
		strategyAnalysis.runDagstrateginStrategies();
		log.info("syncTier1 completed");
	}

	/**
	 * Tier 2 — Weekly (SAT morning).
	 * Syncs price history for all active YAHOO securities.
	 */
	public void syncTier2() {
		log.info("syncTier2 started");
		addSecurityPricesFromYahoo();
		log.info("syncTier2 completed");
	}

	/**
	 * Tier 3 — Monthly (1st of month).
	 * Updates fundamental metadata (Beta, PEG, sector, etc.) for all active stocks.
	 */
	public void syncTier3() {
		log.info("syncTier3 started");
		updateSecurityMetaData();
		log.info("syncTier3 completed");
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
		log.info("dataSetRepository deleted");
		recommendationTrendRepository.deleteAllInBatch();
		log.info("recommendationTrendRepository deleted");
		securityRepository.deleteAllInBatch();
		log.info("securityRepository deleted");
		securityPriceRepository.deleteAllInBatch();
		log.info("securityPriceRepository deleted");
		strategyIndicatorValueRepository.deleteAllInBatch();
		log.info("strategyIndicatorValueRepository deleted");
		tradesRepository.deleteAllInBatch();
		log.info("tradesRepository deleted");
		featuredStrategyRepository.deleteAllInBatch();
		log.info("featuredStrategyRepository deleted");
		strategyPerformanceRepository.deleteAllInBatch();
		log.info("featuredStrategyRepository deleted");

	}
	
	/**
	 * Insert or add DataSet and its securities defined in csv-file.
	 * 
	 */
	private void addDataSetAndSecurities() {
		dataManager.addDatasetSecuritiesIntoDatabase();
		log.info("addDataSetAndSecurities executed");
	}
	
	private void addSecurityPricesFromYahoo() {
		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);
		log.info("addSecurityPricesFromYahoo executed");
	}

	private void addSecurityPricesFromCoinbase() {
		dataManager.addSecurityPricesIntoDatabase(Database.COINBASE);
		log.info("addSecurityPricesFromCoinbase executed");
	}

	public void addSecurityPriceFromDatabase(String security, Database database) {
		dataManager.insertSecurityPricesIntoDatabase(database, security);
	}

	public void updateSecurityMetaData() {
		dataManager.updateSecurityMetaData(Database.YAHOO);
	}

	public void updateSecurityMetaData(String security) {
		dataManager.updateSecurityMetaData(Database.YAHOO, security);
	}

	/**
	 * Run all stratregies on all securities.
	 * This will insert result into {@linkplain FeaturedStrategyRepository}
	 * 
	 */
	private void runAllStrategies() {
		strategyAnalysis.run(null, null);
	}

	public void runStrategy(String strategy, Long securityId) {
		strategyAnalysis.run(strategy, securityId);
	}

	private void runChooseBestStrategy() {
		strategyAnalysis.runChooseBestStrategy();
	}

	public enum ACTION {
		LOAD_DATA,
		RUN_STRATEGY
	}

}
