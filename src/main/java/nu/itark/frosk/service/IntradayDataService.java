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
 * Tier-0 intraday data pipeline for OMX30 constituent stocks.
 *
 * <p>On every 10-minute scheduler tick this service fetches 15-minute bars
 * for each security in the OMX30 dataset, persists them, prunes old bars,
 * and builds ta4j {@link BarSeries} instances for strategy evaluation.
 */
@Service
@Slf4j
public class IntradayDataService {

    private static final String DATASET_NAME = "OMX30";
    private static final String INTERVAL_CODE = "15m";
    private static final Duration BAR_DURATION = Duration.ofMinutes(15);
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Value("${intraday.retention.days:7}")
    private int retentionDays;

    @Autowired
    private YahooFinanceDirectClient yahooFinanceClient;

    @Autowired
    private IntradayBarRepository intradayBarRepository;

    @Autowired
    private DataSetRepository dataSetRepository;

    /**
     * Fetch fresh 15-minute bars for all OMX30 securities, persist them,
     * prune old bars, and return a map of Security to ta4j BarSeries.
     *
     * @return map keyed by Security; empty map if the OMX30 dataset is not found.
     */
    public Map<Security, BarSeries> syncAndBuildAllSeries() {
        DataSet omx30 = dataSetRepository.findByName(DATASET_NAME);
        if (omx30 == null) {
            log.warn("IntradayDataService: dataset '{}' not found", DATASET_NAME);
            return Collections.emptyMap();
        }

        List<Security> securities = omx30.getSecurities();
        log.info("IntradayDataService: syncing 15m bars for {} OMX30 securities", securities.size());

        for (int i = 0; i < securities.size(); i++) {
            syncSecurity(securities.get(i));
            if (i < securities.size() - 1) {
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        pruneOldBars();

        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (Security security : securities) {
            BarSeries series = buildSeriesFromDb(security.getId(), security.getName());
            if (series.getBarCount() > 0) {
                result.put(security, series);
            }
        }

        log.info("IntradayDataService: built {} non-empty BarSeries from OMX30", result.size());
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
