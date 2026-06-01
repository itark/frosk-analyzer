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
 *
 * Budget: ~8,002 / 10,000 req/month on the yahoo-finance15 free plan.
 * (Tier 0 adds ~1,122 req/month — 1 req × ~51 ticks/day × 22 trading days)
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
}
