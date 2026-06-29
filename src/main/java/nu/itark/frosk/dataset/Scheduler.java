package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.HighLander;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled jobs — four-tier sync strategy.
 *
 * Tier 0 (intraday, MON-FRI every 10 min 08:30-17:30 Swedish time):
 *                              Fetch 15m OMX30 bars + run intraday strategies  ~29 req/run
 * Tier 1 (daily, MON-FRI 18:00):  OMXS30 price sync + HedgeIndex update   ~40 req/run
 * Tier 2 (weekly, SAT 06:00):     Full universe price sync                  ~900 req/run
 * Tier 3 (monthly, 1st 07:00):    Fundamental metadata update               ~1,800 req/run
 * Crypto (daily, 00:30):           Coinbase price sync + strategy run
 *
 * All Yahoo data is fetched via YahooFinanceDirectClient (direct calls to
 * Yahoo's public v8/v10 endpoints) — no API key, no cost, no monthly quota.
 * The tier split now serves load-spreading and data freshness, not a request
 * budget. Crypto sync uses the Coinbase API directly.
 */
@Configuration
@EnableScheduling
@Slf4j
public class Scheduler {

	@Autowired
	private HighLander highLander;

	/**
	 * Tier 0 — fetch 15-minute OMX30 bars and run intraday strategies
	 * every 10 minutes during Stockholm market hours (08:30–17:30, Mon–Fri).
	 */
	@Scheduled(cron = "${scheduler.tier0.cron}")
	public void tier0IntradaySync() {
		log.info("Scheduler::tier0IntradaySync starting");
		highLander.syncTier0();
		log.info("Scheduler::tier0IntradaySync completed");
	}

	/** Tier 1 — sync OMXS30 + HedgeIndex every weekday after market close. */
	@Scheduled(cron = "${scheduler.tier1.cron}")
	public void tier1DailySync() {
		log.info("Scheduler::tier1DailySync starting");
		highLander.syncTier1();
		log.info("Scheduler::tier1DailySync completed");
	}

	/** Tier 2 — sync full universe every Saturday morning. */
	@Scheduled(cron = "${scheduler.tier2.cron}")
	public void tier2WeeklySync() {
		log.info("Scheduler::tier2WeeklySync starting");
		highLander.syncTier2();
		log.info("Scheduler::tier2WeeklySync completed");
	}

	/** Tier 3 — update fundamental metadata on the 1st of each month. */
	@Scheduled(cron = "${scheduler.tier3.cron}")
	public void tier3MonthlyMetadata() {
		log.info("Scheduler::tier3MonthlyMetadata starting");
		highLander.syncTier3();
		log.info("Scheduler::tier3MonthlyMetadata completed");
	}

	/**
	 * Crypto — sync Coinbase prices and run strategies daily at 00:30.
	 * Crypto trades 24/7, so one daily sync captures the full previous day.
	 * Uses the Coinbase API directly.
	 */
	@Scheduled(cron = "${scheduler.crypto.cron:0 30 0 * * *}")
	public void cryptoDailySync() {
		log.info("Scheduler::cryptoDailySync starting");
		highLander.syncCrypto();
		log.info("Scheduler::cryptoDailySync completed");
	}

	/**
	 * Crypto intraday — fetch 15m Coinbase candles for the configured products.
	 * Disabled by default ("-"); the crypto profile enables it every 15 minutes,
	 * around the clock (crypto trades 24/7).
	 */
	@Scheduled(cron = "${scheduler.crypto.intraday.cron:-}")
	public void cryptoIntradaySync() {
		log.info("Scheduler::cryptoIntradaySync starting");
		highLander.syncCryptoIntraday();
		log.info("Scheduler::cryptoIntradaySync completed");
	}
}
