package nu.itark.frosk.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intraday (15m) data pipeline for the {@code kraken-futures} profile.
 *
 * <p>Fetches OHLCV candles from the Kraken Futures charts API
 * ({@code /api/charts/v1/trade/{symbol}/15m?from={sec}&to={sec}}) and persists
 * them into the shared {@code intraday_bar} table. The underlying ta4j
 * {@link BarSeries} construction is identical to {@link CryptoIntradayDataService}
 * so that {@link CryptoIntradayStrategyRunner} is completely data-source agnostic.
 *
 * <h3>Candle timestamp convention</h3>
 * The Kraken Futures charts API returns candle {@code time} in <em>milliseconds</em>,
 * but takes the {@code from}/{@code to} query params in <em>seconds</em>.
 * We store epoch-seconds in {@code intraday_bar.bar_timestamp} (matching the
 * Coinbase pipeline's convention), so the candle time is divided by 1 000 on ingest.
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesIntradayDataService implements IntradayDataService {

    private static final String INTERVAL_CODE  = "15m";
    private static final Duration BAR_DURATION  = Duration.ofMinutes(15);
    private static final Duration DAY_DURATION  = Duration.ofDays(1);
    private static final ZoneId   UTC           = ZoneId.of("UTC");
    /** Kraken Futures allows up to 5000 candles per request; keep chunks reasonable. */
    private static final int MAX_CANDLES_PER_REQUEST = 500;
    private static final long REQUEST_PAUSE_MILLIS   = 200;
    /** Minimum days of daily-close history to keep for the regime SMA. */
    private static final int MIN_DAILY_BACKFILL_DAYS = 400;

    @Value("${crypto.intraday.products:PF_XBTUSD,PF_ETHUSD,PF_SOLUSD}")
    private List<String> products;

    @Value("${intraday.retention.days:30}")
    private int retentionDays;

    /** Regime product — daily closes for this symbol feed {@link CryptoRegimeService}. */
    @Value("${crypto.regime.product:PF_XBTUSD}")
    private String regimeProduct;

    @Value("${crypto.regime.sma.period:50}")
    private int regimeSmaPeriod;

    /**
     * Kraken Futures charts (OHLC) API base URL. This API is only served from the
     * live host — {@code demo-futures.kraken.com} 301-redirects the whole
     * {@code /api/charts} path to a marketing page. Candle data is public, so this
     * is safe even when the derivatives/trading API points at the demo host.
     *
     * <p>Endpoint shape: {@code {baseUrl}/trade/{symbol}/15m?from={sec}&to={sec}}
     */
    @Value("${kraken.futures.history.baseUrl:https://futures.kraken.com/api/charts/v1}")
    private String historyBaseUrl;

    @Autowired
    private IntradayBarRepository intradayBarRepository;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private SecurityPriceRepository securityPriceRepository;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── IntradayDataService ─────────────────────────────────────────────────

    @Override
    public Map<Security, BarSeries> syncAndBuildAllSeries() {
        syncDailyCloses();
        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (String rawId : products) {
            String productId = rawId.trim();
            Security security = securityRepository.findByName(productId);
            if (security == null) {
                log.warn("KrakenFuturesIntradayDataService: security '{}' not found — " +
                         "KrakenFuturesSecurityInitializer should have created it", productId);
                continue;
            }
            try {
                syncProduct(security);
            } catch (Exception e) {
                log.error("KrakenFuturesIntradayDataService: sync failed for {}: {}", productId, e.getMessage());
            }
            BarSeries series = buildSeriesFromDb(security.getId());
            if (series.getBarCount() > 0) {
                result.put(security, series);
            }
        }
        pruneOldBars();
        log.info("KrakenFuturesIntradayDataService: built {} BarSeries for {}", result.size(), products);
        return result;
    }

    @Override
    public Map<Security, BarSeries> buildAllSeriesFromDb() {
        Map<Security, BarSeries> result = new LinkedHashMap<>();
        for (String rawId : products) {
            Security security = securityRepository.findByName(rawId.trim());
            if (security == null) continue;
            BarSeries series = buildSeriesFromDb(security.getId());
            if (series.getBarCount() > 0) result.put(security, series);
        }
        return result;
    }

    // ── private ─────────────────────────────────────────────────────────────

    /**
     * Populate {@code security_price} with daily closes for the regime product.
     *
     * <p>The {@code kraken-futures} profile has no nightly price feed (Coinbase and
     * Yahoo sync are both disabled), so without this {@link CryptoRegimeService}
     * finds an empty {@code security_price} table, the regime cache stays empty and
     * the regime gate fails closed — long entries never fire and short entries are
     * never regime-blocked. One {@code 1d} request per cycle keeps it current.
     */
    private void syncDailyCloses() {
        Security regime = securityRepository.findByName(regimeProduct);
        if (regime == null) {
            log.warn("KrakenFuturesIntradayDataService: regime product '{}' not found — skipping daily close sync",
                    regimeProduct);
            return;
        }

        long now  = Instant.now().getEpochSecond();
        long days = Math.max(MIN_DAILY_BACKFILL_DAYS, regimeSmaPeriod * 4L);
        long from = now - days * DAY_DURATION.getSeconds();

        String url = historyBaseUrl + "/trade/" + regimeProduct + "/1d?from=" + from + "&to=" + now;
        CandleResponse response;
        try {
            response = restTemplate.getForObject(url, CandleResponse.class);
        } catch (Exception e) {
            log.warn("KrakenFuturesIntradayDataService: daily close request failed for {}: {}",
                    regimeProduct, e.getMessage());
            return;
        }
        if (response == null || response.getCandles() == null) {
            log.warn("KrakenFuturesIntradayDataService: no daily candles returned for {}", regimeProduct);
            return;
        }

        int inserted = 0;
        for (KrakenCandle c : response.getCandles()) {
            long epochSec = c.getTime() / 1000L;
            // Skip the still-forming current day
            if (epochSec + DAY_DURATION.getSeconds() > now) continue;

            Date ts = new Date(c.getTime());
            if (securityPriceRepository.findBySecurityIdAndTimestamp(regime.getId(), ts) != null) continue;

            securityPriceRepository.save(new SecurityPrice(
                    regime.getId(), ts,
                    parsePrice(c.getOpen()), parsePrice(c.getHigh()),
                    parsePrice(c.getLow()),  parsePrice(c.getClose()),
                    parseVolume(c.getVolume())));
            inserted++;
        }
        if (inserted > 0) {
            cryptoRegimeService.clearCache();
            log.info("KrakenFuturesIntradayDataService: inserted {} daily closes for {} — regime cache cleared",
                    inserted, regimeProduct);
        }
    }

    private void syncProduct(Security security) {
        long retentionStart = Instant.now().minus(Duration.ofDays(retentionDays)).getEpochSecond();
        long now             = Instant.now().getEpochSecond();

        IntradayBar latest   = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
        IntradayBar earliest = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampAsc(security.getId());

        // Backfill gap behind earliest stored bar (e.g. after retention window widened)
        if (earliest != null && earliest.getBarTimestamp() > retentionStart + BAR_DURATION.getSeconds()) {
            fetchRange(security, retentionStart, earliest.getBarTimestamp());
        }
        long from = latest != null
                ? Math.max(latest.getBarTimestamp() + BAR_DURATION.getSeconds(), retentionStart)
                : retentionStart;
        fetchRange(security, from, now);
    }

    /**
     * Fetch and store candles in [{@code fromEpochSec}, {@code untilEpochSec}] using
     * the Kraken Futures charts API. Requests are chunked to respect the per-request
     * candle limit and separated by a short pause.
     */
    private void fetchRange(Security security, long fromEpochSec, long untilEpochSec) {
        long now = Instant.now().getEpochSecond();
        int inserted = 0;
        long chunkSec = (long) MAX_CANDLES_PER_REQUEST * BAR_DURATION.getSeconds();

        for (long start = fromEpochSec; start < untilEpochSec; start += chunkSec) {
            long end = Math.min(start + chunkSec, untilEpochSec);

            // Charts API: /trade/{symbol}/{resolution}, from/to in SECONDS
            String url = historyBaseUrl + "/trade/" + security.getName() + "/15m"
                    + "?from=" + start
                    + "&to="   + end;

            CandleResponse response = null;
            try {
                response = restTemplate.getForObject(url, CandleResponse.class);
            } catch (Exception e) {
                log.warn("KrakenFuturesIntradayDataService: candle request failed for {} [{}-{}]: {}",
                        security.getName(), start, end, e.getMessage());
            }
            pauseBetweenRequests();

            // Charts API response has no "result" field — a non-null candles array is success.
            if (response == null || response.getCandles() == null) {
                continue;
            }

            for (KrakenCandle c : response.getCandles()) {
                // Kraken returns time in milliseconds → convert to epoch seconds
                long epochSec = c.getTime() / 1000L;
                // Skip the still-forming candle
                if (epochSec + BAR_DURATION.getSeconds() > now) continue;

                if (!intradayBarRepository.existsBySecurityIdAndBarTimestampAndIntervalCode(
                        security.getId(), epochSec, INTERVAL_CODE)) {
                    intradayBarRepository.save(new IntradayBar(
                            security.getId(),
                            epochSec,
                            INTERVAL_CODE,
                            parseBd(c.getOpen()),
                            parseBd(c.getHigh()),
                            parseBd(c.getLow()),
                            parseBd(c.getClose()),
                            parseVolume(c.getVolume())
                    ));
                    inserted++;
                }
            }
        }
        if (inserted > 0) {
            log.info("KrakenFuturesIntradayDataService: inserted {} new 15m bars for {}",
                    inserted, security.getName());
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

    private void pruneOldBars() {
        long cutoff = Instant.now().minus(Duration.ofDays(retentionDays)).getEpochSecond();
        int pruned = intradayBarRepository.deleteOlderThan(cutoff);
        if (pruned > 0) {
            log.info("KrakenFuturesIntradayDataService: pruned {} bars older than {} days", pruned, retentionDays);
        }
    }

    private void pauseBetweenRequests() {
        try { Thread.sleep(REQUEST_PAUSE_MILLIS); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static BigDecimal parseBd(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    /** Kraken sometimes returns prices with 20 decimal places; {@code security_price} is scale 6. */
    private static BigDecimal parsePrice(String s) {
        return parseBd(s).setScale(6, RoundingMode.HALF_UP);
    }

    private static long parseVolume(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return new BigDecimal(s).longValue(); } catch (NumberFormatException e) { return 0L; }
    }

    // ── Kraken candle response POJOs ─────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CandleResponse {
        private List<KrakenCandle> candles;
        private boolean more_candles;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KrakenCandle {
        /** Candle open time in milliseconds (Kraken Futures charts API convention). */
        private long   time;
        private String open;
        private String high;
        private String low;
        private String close;
        private String volume;
    }
}
