package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.ta4j.core.BarSeries;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persisting indicator values for a multi-year daily backtest is what drove
 * strat_indicator_value to 6.9M rows (see StrategyExecutor's persistIndicatorValues
 * javadoc) — not repeated runs, since delete-before-reinsert already bounds those.
 * This proves the retention-window filter added alongside re-enabling the flag
 * actually caps what gets written, using a short 30-day window against a security
 * with years of history so the difference is unambiguous.
 */
@TestPropertySource(properties = {
        "frosk.strategy.persist.indicator.values=true",
        // 120, not 30: the retention cutoff is real wall-clock time
        // (Instant.now()), but the test DB's price data is a frozen snapshot
        // (latest bar 2026-06-12 as of writing) — a short window relative to
        // today would filter out 100% of it regardless of whether the logic
        // being tested works. 120 days comfortably covers that gap while
        // staying far short of the ~3-year full series this test picks.
        "frosk.strategy.persist.indicator.values.retention.days=120"
})
public class TestJIndicatorValueRetentionWindow extends BaseIntegrationTest {

    @Autowired
    StrategyExecutor strategyExecutor;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    StrategiesMap strategiesMap;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    StrategyIndicatorValueRepository indicatorValueRepo;

    @Autowired
    SecurityRepository securityRepository;

    @Test
    public void onlyRecentIndicatorPointsArePersisted() {
        List<BarSeries> seriesList = barSeriesService.getDataSet(Database.YAHOO);
        assertFalse(seriesList.isEmpty(), "No YAHOO series in test database");

        // Pick a series with real history — the whole point is proving a
        // multi-year backtest does NOT persist multi-year indicator rows.
        BarSeries longSeries = seriesList.stream()
                .filter(s -> !s.getBarData().isEmpty() && s.getBarCount() > 100)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No series with >100 bars in test database"));
        // BarSeries is named by security ID (StrategyExecutor.execute resolves it the
        // same way), not by ticker — resolve the real ticker for the FeaturedStrategy lookup.
        Security security = securityRepository.findById(Long.valueOf(longSeries.getName())).orElseThrow();
        String securityName = security.getName();

        strategyExecutor.execute("NewsBreakoutStrategy", List.of(longSeries));

        FeaturedStrategy fs = featuredStrategyRepository
                .findByNameAndSecurityName("NewsBreakoutStrategy", securityName);
        assertNotNull(fs, "expected a FeaturedStrategy row for " + securityName);

        try {
            List<StrategyIndicatorValue> saved = indicatorValueRepo.findByFeaturedStrategyId(fs.getId());
            assertFalse(saved.isEmpty(), "expected some indicator rows to be persisted");

            // 120-day window, 5 indicators (close/shortSma10/ema9/ema21/gapPct), daily
            // bars — at most ~85 trading days in 120 calendar days, so well under
            // 500 rows total. The full series spans ~3 years (see class javadoc),
            // so this bound only holds if the window filter is actually working.
            assertTrue(saved.size() < 500,
                    "expected retention window to cap rows well under the full series length, got " + saved.size());

            Date cutoff = Date.from(Instant.now().minus(121, ChronoUnit.DAYS));
            long tooOld = saved.stream().filter(iv -> iv.getDate().before(cutoff)).count();
            assertTrue(tooOld == 0, "found " + tooOld + " indicator rows older than the 120-day retention window");
        } finally {
            indicatorValueRepo.deleteByFeaturedStrategyId(fs.getId());
        }
    }
}
