package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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

	@Value("${frosk.run.manedsportfolj:false}")
	private boolean runManedsportfolj;

	@Value("${frosk.run.trendfollowing:false}")
	private boolean runTrendFollowing;

	@Value("${frosk.run.intraday:true}")
	private boolean runIntraday;

	@Value("${frosk.run.crypto:false}")
	private boolean runCrypto;

	@Value("${frosk.updatesecuritymetadata}")
	private boolean updateSecurityMetaData;

	/** Number of slices the metadata refresh is spread over; 1 = full sweep each run. */
	@Value("${frosk.metadata.slice.count:20}")
	private int metadataSliceCount;

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
	nu.itark.frosk.service.EarningsCalendarService earningsCalendarService;

	@Autowired
	BarSeriesService barSeriesService;

	@Autowired
	HedgeIndexService hedgeIndexService;

	@Autowired
	PortfolioService portfolioService;

	@Autowired
	YAHOODataManager yahooDataManager;

	@Autowired
	nu.itark.frosk.service.IntradayStrategyRunner intradayStrategyRunner;

	@Autowired
	nu.itark.frosk.service.CryptoIntradayStrategyRunner cryptoIntradayStrategyRunner;

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
		log.info("runManedsportfolj:{}",runManedsportfolj);

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
			yahooDataManager.syncronizeByDataset("OMX30");
			strategyAnalysis.runDagstrateginStrategies();
		}
		if (runManedsportfolj) {
			yahooDataManager.syncronizeActiveSwedish();
			strategyAnalysis.runMånadsportföljStrategies();
		}
		if (runTrendFollowing) {
			strategyAnalysis.runTrendFollowingStrategies();
		}
	}

	private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
	private static final LocalTime INTRADAY_OPEN  = LocalTime.of(8, 30);
	private static final LocalTime INTRADAY_CLOSE = LocalTime.of(17, 30);

	/**
	 * Tier 0 — Intraday (every 10 minutes, MON-FRI 08:30-17:30 Swedish time).
	 * Fetches latest 15-minute bars for OMX30 dataset and runs intraday strategies.
	 * Emits BUY/SELL signals to the intraday_signal table for human review.
	 */
	public void syncTier0() {
		syncTier0(false);
	}
 
	public void syncTier0(boolean force) {
		if (!runIntraday) {
			log.info("syncTier0 skipped — frosk.run.intraday=false");
			return;
		}
		if (!force) {
			LocalTime now = ZonedDateTime.now(STOCKHOLM).toLocalTime();
			if (now.isBefore(INTRADAY_OPEN) || now.isAfter(INTRADAY_CLOSE)) {
				log.info("syncTier0 skipped — outside Swedish market hours ({}, window {}–{})",
						now, INTRADAY_OPEN, INTRADAY_CLOSE);
				return;
			}
		}
		log.info("syncTier0 started (force={})", force);
		intradayStrategyRunner.run();
		portfolioService.buildIntraday();
		log.info("syncTier0 completed");
	}

	/**
	 * Tier 1 — Daily (MON-FRI after market close).
	 * Syncs OMXS30 constituents + runs HedgeIndex strategies (which syncs macro tickers).
	 */
	public void syncTier1() {
		log.info("syncTier1 started");
		yahooDataManager.syncronizeByDataset("OMX30");
		yahooDataManager.syncronizeActiveSwedish();
		strategyAnalysis.runHedgeIndexStrategies();
		strategyAnalysis.runDagstrateginStrategies();
		// Daily portfolio snapshot. Moved here from Tier 2 on 2026-08-05: Tier 2 runs
		// Saturdays only, so the snapshot was up to six days stale on any given weekday
		// while the prices and signals feeding it refreshed daily. build() is idempotent
		// per day, so the Saturday call in Tier 2 is harmless overlap, not a double build.
		portfolioService.build();
		// Append-only capture of the forward earnings calendar and analyst consensus.
		// Runs last and swallows its own failures: it is data collection for a future
		// experiment and must never break the sync it rides on. Yahoo overwrites these
		// values silently, so a day missed here is a day of revision history lost for good.
		try {
			earningsCalendarService.captureAll();
		} catch (Exception e) {
			log.warn("syncTier1: earnings capture failed — {}", e.toString());
		}
		log.info("syncTier1 completed");
	}

	/**
	 * Tier 2 — Weekly (SAT morning). Computation only; no price sync.
	 * Re-runs Månadsportföljen (SwedishLongTermMomentumStrategy) and rebuilds the
	 * daily portfolio snapshot on prices Tier 1 already refreshed during the week.
	 */
	public void syncTier2() {
		log.info("syncTier2 started");
		// Price sync deliberately removed 2026-08-05. It was redundant: Tier 1 runs
		// syncronizeActiveSwedish() every weekday over all active .ST securities, and
		// runHedgeIndexStrategies() covers the macro tickers — together that is the
		// whole active YAHOO universe. Fetching is incremental (getStocks() resumes
		// from the last stored bar), so daily coverage costs one bar per security,
		// not a re-download of history. Tier 2 now exists only for the weekly
		// computation below, which genuinely belongs on a weekly cadence.
		strategyAnalysis.runMånadsportföljStrategies();
		// Trend following rebalances monthly by design; running it weekly only refreshes
		// the signal, it does not add turnover — the strategy is flat-or-long on a
		// 252-day lookback, which cannot flip more than a few times a year.
		strategyAnalysis.runTrendFollowingStrategies();
		portfolioService.build();
		log.info("syncTier2 completed");
	}

	/**
	 * Tier 3 — One metadata slice per weekday (Beta, PEG, sector, analyst trend).
	 * Every security is still refreshed about monthly; only the traffic shape changed.
	 */
	public void syncTier3() {
		// One slice per run instead of the whole universe. With the default 20 slices
		// on a weekday schedule every security is refreshed about monthly — the old
		// cadence — at ~140 requests a day instead of ~2,800 at once. Slice chosen from
		// the day of year, so it is stateless: a missed day self-corrects next cycle.
		int slice = Math.floorMod(java.time.LocalDate.now().getDayOfYear(), metadataSliceCount);
		log.info("syncTier3 started (slice {}/{})", slice, metadataSliceCount);
		dataManager.updateSecurityMetaData(Database.YAHOO, metadataSliceCount, slice);
		log.info("syncTier3 completed");
	}

	/**
	 * Crypto sync — syncs Coinbase price data and runs strategies on crypto securities.
	 * Runs independently of OMX market hours (crypto is 24/7).
	 * Does NOT warm HedgeIndex cache — crypto is not gated by OMX macro signals.
	 *
	 * In the two-process architecture, this method runs in the crypto process
	 * where the main (and only) datasource points to the crypto H2 database.
	 */
	public void syncCrypto() {
		if (!runCrypto) {
			log.info("syncCrypto skipped — frosk.run.crypto=false");
			return;
		}
		log.info("syncCrypto started");
		addSecurityPricesFromCoinbase();
		strategyAnalysis.runCryptoStrategies();
		log.info("syncCrypto completed");
	}

	/**
	 * Crypto intraday — fetch 15m Coinbase candles for the configured products
	 * ({@code crypto.intraday.products}). Runs around the clock; crypto has no
	 * market hours. Strategy evaluation on these series is a separate step —
	 * this keeps the bar history current.
	 */
	public void syncCryptoIntraday() {
		if (!runCrypto) {
			log.info("syncCryptoIntraday skipped — frosk.run.crypto=false");
			return;
		}
		log.info("syncCryptoIntraday started");
		cryptoIntradayStrategyRunner.run();
		portfolioService.buildIntraday();
		log.info("syncCryptoIntraday completed");
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

	public void addSecurityPriceFromDatabase(Long securityId) {
		Security security = securityRepository.findById(securityId).get();
		Database database = Database.valueOf(security.getDatabase());
		dataManager.insertSecurityPricesIntoDatabase(database, security.getName());
	}

	public void updateSecurityMetaData() {
		dataManager.updateSecurityMetaData(Database.YAHOO);
	}

	public void updateSecurityMetaData(Long securityId) {
		Security security = securityRepository.findById(securityId).get();
		// Route by the security's own database. Fundamental metadata (beta, PEG,
		// income statement, analyst trends) only exists for YAHOO equities;
		// DataManager no-ops for COINBASE, so crypto pairs like SOL-EUR are
		// skipped instead of hitting Yahoo's stock-only quoteSummary (404 + NPE).
		Database database = Database.valueOf(security.getDatabase());
		dataManager.updateSecurityMetaData(database, security);
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
