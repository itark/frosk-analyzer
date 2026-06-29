package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.YahooFinanceDirectClient;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.rapidapi.yhfinance.model.StockHistoryDTO;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.IntradayBarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Tier-0 intraday data pipeline.
 *
 * <p>On every 10-minute scheduler tick this service fetches 15-minute bars
 * for each security in the configured datasets ({@code intraday.datasets}),
 * persists them, prunes old bars, and builds ta4j {@link BarSeries} instances
 * for strategy evaluation.
 */
@Service
@Slf4j
public class IntradayDataService {

    private static final String INTERVAL_CODE = "15m";
    private static final Duration BAR_DURATION = Duration.ofMinutes(15);
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Value("${intraday.datasets:OMX30}")
    private List<String> datasetNames;

    @Value("${intraday.retention.days:7}")
    private int retentionDays;

    @Value("${yahoo.fetch.delay.ms:300}")
    private int fetchDelayMs;

    @Autowired
    private YahooFinanceDirectClient yahooFinanceClient;

    @Autowired
    private IntradayBarRepository intradayBarRepository;

    @Autowired
    private DataSetRepository dataSetRepository;

    /**
     * Fetch fresh 15-minute bars for all securities in the configured datasets,
     * persist them, prune old bars, and return a map of Security to ta4j BarSeries.
     *
     * @return map keyed by Security; empty map if no datasets are found.
     */
    public Map<Security, BarSeries> syncAndBuildAllSeries() {
        List<Security> allSecurities = collectSecurities();
        if (allSecurities.isEmpty()) {
            log.warn("IntradayDataService: no securities found across datasets {}", datasetNames);
            return Collections.emptyMap();
        }

        log.info("IntradayDataService: syncing 15m bars for {} securities from datasets {}",
                allSecurities.size(), datasetNames);

        for (int i = 0; i < allSecurities.size(); i++) {
            syncSecurity(allSecurities.get(i));
            if (i < allSecurities.size() - 1) {
                sleepBetweenFetches();
            }
        }

        pruneOldBars();

        return buildSeriesMap(allSecurities);
    }

    /**
     * Politeness pause between consecutive Yahoo ticker fetches. Yahoo's v8
     * endpoint is free but unofficial and can throttle/IP-block bursty traffic.
     * Controlled by {@code yahoo.fetch.delay.ms} (set to 0 to disable).
     */
    private void sleepBetweenFetches() {
        if (fetchDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(fetchDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Builds BarSeries for all securities in the configured datasets from
     * already-persisted bars only — no network sync, no pruning. Used by
     * backtest/report tooling that must run offline.
     */
    public Map<Security, BarSeries> buildAllSeriesFromDb() {
        List<Security> allSecurities = collectSecurities();
        if (allSecurities.isEmpty()) {
            log.warn("IntradayDataService: no securities found across datasets {}", datasetNames);
            return Collections.emptyMap();
        }
        return buildSeriesMap(allSecurities);
    }

    private List<Security> collectSecurities() {
        List<Security> allSecurities = new ArrayList<>();
        for (String datasetName : datasetNames) {
            DataSet ds = dataSetRepository.findByName(datasetName.trim());
            if (ds == null) {
                log.warn("IntradayDataService: dataset '{}' not found", datasetName);
                continue;
            }
            allSecurities.addAll(ds.getSecurities());
        }
        return allSecurities;
    }

    private Map<Security, BarSeries> buildSeriesMap(List<Security> securities) {
        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (Security security : securities) {
            BarSeries series = buildSeriesFromDb(security.getId(), security.getName());
            if (series.getBarCount() > 0) {
                result.put(security, series);
            }
        }
        log.info("IntradayDataService: built {} non-empty BarSeries from {}", result.size(), datasetNames);
        return result;
    }

    private void syncSecurity(Security security) {
        Map<String, StockHistoryDTO.StockData> rawBars;
        try {
            rawBars = yahooFinanceClient.getIntradayBars(
                    security.getName(), INTERVAL_CODE, "5d");
        } catch (Exception e) {
            log.error("IntradayDataService: failed to fetch 15m bars for {}: {}",
                    security.getName(), e.getMessage());
            return;
        }

        if (rawBars == null || rawBars.isEmpty()) {
            log.debug("IntradayDataService: empty response for {}", security.getName());
            return;
        }

        int inserted = 0;
        for (StockHistoryDTO.StockData bar : rawBars.values()) {
            long epochSec = bar.getDateUtc();
            if (epochSec <= 0) continue;
            if (bar.getClose() == 0.0 || bar.getOpen() == 0.0) continue;

            if (!intradayBarRepository.existsBySecurityIdAndBarTimestampAndIntervalCode(
                    security.getId(), epochSec, INTERVAL_CODE)) {
                IntradayBar ib = new IntradayBar(
                        security.getId(),
                        epochSec,
                        INTERVAL_CODE,
                        BigDecimal.valueOf(bar.getOpen()),
                        BigDecimal.valueOf(bar.getHigh()),
                        BigDecimal.valueOf(bar.getLow()),
                        BigDecimal.valueOf(bar.getClose()),
                        bar.getVolume()
                );
                intradayBarRepository.save(ib);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("IntradayDataService: inserted {} new 15m bars for {}", inserted, security.getName());
        }
    }

    private void pruneOldBars() {
        long cutoff = Instant.now()
                .minus(Duration.ofDays(retentionDays))
                .getEpochSecond();
        int pruned = intradayBarRepository.deleteOlderThan(cutoff);
        if (pruned > 0) {
            log.info("IntradayDataService: pruned {} bars older than {} days", pruned, retentionDays);
        }
    }

    private BarSeries buildSeriesFromDb(Long securityId, String securityName) {
        long cutoff = Instant.now()
                .minus(Duration.ofDays(retentionDays))
                .getEpochSecond();

        List<IntradayBar> bars = intradayBarRepository
                .findBySecurityIdAndIntervalCodeAndBarTimestampGreaterThanOrderByBarTimestampAsc(
                        securityId, INTERVAL_CODE, cutoff);

        BarSeries series = new BaseBarSeriesBuilder()
                .withName(String.valueOf(securityId))
                .withNumTypeOf(DoubleNum.class)
                .build();

        for (IntradayBar ib : bars) {
            ZonedDateTime endTime = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(ib.getBarTimestamp()), STOCKHOLM)
                    .plus(BAR_DURATION);

            series.addBar(
                    endTime,
                    ib.getOpen().doubleValue(),
                    ib.getHigh().doubleValue(),
                    ib.getLow().doubleValue(),
                    ib.getClose().doubleValue(),
                    ib.getVolume() != null ? (double) ib.getVolume() : 0.0
            );
        }

        return series;
    }
}
