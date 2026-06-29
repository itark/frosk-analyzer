package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Intraday (15m) data pipeline for the crypto process.
 *
 * <p>Pulls 15-minute candles for the configured Coinbase products
 * ({@code crypto.intraday.products}) directly from the Coinbase Advanced API,
 * persists them into the shared {@code intraday_bar}
 * table, and builds ta4j {@link BarSeries} for strategy evaluation.
 *
 * <p>Crypto trades 24/7 — there are no market-hours windows, no opening range
 * and no overnight gaps. Day-anchored indicators (e.g. session VWAP) should be
 * anchored to UTC midnight for crypto series.
 *
 * <p>Coinbase returns at most ~350 candles per request, so backfills are
 * fetched in chunks of {@value #MAX_CANDLES_PER_REQUEST} bars.
 */
@Service
@Slf4j
public class CryptoIntradayDataService {

    private static final String INTERVAL_CODE = "15m";
    private static final Duration BAR_DURATION = Duration.ofMinutes(15);
    private static final int MAX_CANDLES_PER_REQUEST = 300;
    private static final ZoneId UTC = ZoneId.of("UTC");

    /** Top-liquidity products only — thin pairs have spreads that eat any edge. */
    @Value("${crypto.intraday.products:BTC-EUR,ETH-EUR,SOL-EUR}")
    private List<String> products;

    @Value("${intraday.retention.days:30}")
    private int retentionDays;

    @Autowired
    private ProductProxy productProxy;

    @Autowired
    private IntradayBarRepository intradayBarRepository;

    @Autowired
    private SecurityRepository securityRepository;

    /**
     * Fetch fresh 15-minute candles for all configured products, persist them,
     * prune bars older than the retention window, and return a map of
     * Security to ta4j BarSeries.
     */
    public Map<Security, BarSeries> syncAndBuildAllSeries() {
        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (String productId : products) {
            Security security = securityRepository.findByName(productId.trim());
            if (security == null) {
                log.warn("CryptoIntradayDataService: security '{}' not found — run dataset/security setup first", productId);
                continue;
            }
            try {
                syncProduct(security);
            } catch (Exception e) {
                log.error("CryptoIntradayDataService: sync failed for {}: {}", productId, e.getMessage());
            }
            BarSeries series = buildSeriesFromDb(security.getId());
            if (series.getBarCount() > 0) {
                result.put(security, series);
            }
        }
        pruneOldBars();
        log.info("CryptoIntradayDataService: built {} non-empty BarSeries for products {}", result.size(), products);
        return result;
    }

    private void syncProduct(Security security) {
        long retentionStart = Instant.now().minus(Duration.ofDays(retentionDays)).getEpochSecond();
        long now = Instant.now().getEpochSecond();
        IntradayBar latest = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
        IntradayBar earliest = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampAsc(security.getId());

        // Backfill missing history behind the earliest stored bar (e.g. after
        // the retention window was widened), then fetch forward from the latest.
        if (earliest != null && earliest.getBarTimestamp() > retentionStart + BAR_DURATION.getSeconds()) {
            fetchRange(security, retentionStart, earliest.getBarTimestamp());
        }
        long from = latest != null
                ? Math.max(latest.getBarTimestamp() + BAR_DURATION.getSeconds(), retentionStart)
                : retentionStart;
        fetchRange(security, from, now);
    }

    private void fetchRange(Security security, long from, long until) {
        long now = Instant.now().getEpochSecond();
        int inserted = 0;
        long chunkSeconds = MAX_CANDLES_PER_REQUEST * BAR_DURATION.getSeconds();
        for (long start = from; start < until; start += chunkSeconds) {
            long end = Math.min(start + chunkSeconds, until);
            Candles candles = productProxy.getPublicCandles(security.getName(),
                    Instant.ofEpochSecond(start), Instant.ofEpochSecond(end), Granularity.FIFTEEN_MINUTE);
            if (candles == null || candles.getCandles() == null) {
                continue;
            }
            for (Candle candle : candles.getCandles()) {
                if (candle.getStart() == null || candle.getClose() == null) {
                    continue;
                }
                long epochSec = candle.getStart().getEpochSecond();
                // Skip the still-forming candle — a partial bar persisted now would
                // never be corrected, since existing timestamps are not re-fetched.
                if (epochSec + BAR_DURATION.getSeconds() > now) {
                    continue;
                }
                if (!intradayBarRepository.existsBySecurityIdAndBarTimestampAndIntervalCode(
                        security.getId(), epochSec, INTERVAL_CODE)) {
                    intradayBarRepository.save(new IntradayBar(
                            security.getId(),
                            epochSec,
                            INTERVAL_CODE,
                            candle.getOpen(),
                            candle.getHigh(),
                            candle.getLow(),
                            candle.getClose(),
                            candle.getVolume() != null ? candle.getVolume().longValue() : 0L
                    ));
                    inserted++;
                }
            }
        }
        if (inserted > 0) {
            log.info("CryptoIntradayDataService: inserted {} new 15m bars for {}", inserted, security.getName());
        }
    }

    /**
     * Builds BarSeries from already-persisted bars only — no network sync,
     * no pruning. Used by backtest/report tooling that must run offline.
     */
    public Map<Security, BarSeries> buildAllSeriesFromDb() {
        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (String productId : products) {
            Security security = securityRepository.findByName(productId.trim());
            if (security == null) {
                continue;
            }
            BarSeries series = buildSeriesFromDb(security.getId());
            if (series.getBarCount() > 0) {
                result.put(security, series);
            }
        }
        return result;
    }

    private void pruneOldBars() {
        long cutoff = Instant.now().minus(Duration.ofDays(retentionDays)).getEpochSecond();
        int pruned = intradayBarRepository.deleteOlderThan(cutoff);
        if (pruned > 0) {
            log.info("CryptoIntradayDataService: pruned {} bars older than {} days", pruned, retentionDays);
        }
    }

    private BarSeries buildSeriesFromDb(Long securityId) {
        long cutoff = Instant.now().minus(Duration.ofDays(retentionDays)).getEpochSecond();
        List<IntradayBar> bars = intradayBarRepository
                .findBySecurityIdAndIntervalCodeAndBarTimestampGreaterThanOrderByBarTimestampAsc(
                        securityId, INTERVAL_CODE, cutoff);

        BarSeries series = new BaseBarSeriesBuilder()
                .withName(String.valueOf(securityId))
                .withNumTypeOf(DoubleNum.class)
                .build();
        for (IntradayBar ib : bars) {
            ZonedDateTime endTime = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(ib.getBarTimestamp()), UTC)
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
