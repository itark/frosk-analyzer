package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One-off, read-only research backfill of 15m candles to CSV files.
 *
 * <p>Deliberately does NOT touch {@code intraday_bar}: that table is pruned to
 * {@code intraday.retention.days} on every sync and stores volume as a truncated
 * {@code Long}. The CSVs keep full-precision volume and are never pruned.
 * Resumable: an existing CSV is extended from its last bar. See
 * {@code ~/itark/PREREG_vwap_signal_filters.md} for the analysis this feeds.
 *
 * <p>Output: {@code <dir>/<PRODUCT>.csv} with header {@code ts,open,high,low,close,volume}
 * (ts = bar start, UTC epoch seconds, ascending).
 */
@Service
@Profile("crypto")
@Slf4j
public class CandleBackfillService {

    private static final long BAR_SECONDS = Duration.ofMinutes(15).getSeconds();
    private static final int CANDLES_PER_REQUEST = 300;
    private static final long PAUSE_MILLIS = 200;
    private static final int MAX_ATTEMPTS = 5;

    @Value("${crypto.intraday.products:BTC-EUR}")
    private List<String> products;

    @Value("${crypto.backfill.dir:${user.home}/itark/backfill}")
    private String outDir;

    @Autowired
    private ProductProxy productProxy;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "candle-backfill");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, String> status = new ConcurrentSkipListMap<>();
    private volatile boolean running;

    /** Starts the backfill in the background. Returns false if one is already running. */
    public synchronized boolean start(int days) {
        if (running) {
            return false;
        }
        running = true;
        status.clear();
        executor.submit(() -> {
            try {
                run(days);
            } catch (Exception e) {
                log.error("CandleBackfillService: aborted: {}", e.getMessage(), e);
                status.put("_error", String.valueOf(e));
            } finally {
                running = false;
            }
        });
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public Map<String, String> getStatus() {
        return status;
    }

    public String getOutDir() {
        return outDir;
    }

    private void run(int days) throws IOException {
        Path dir = Path.of(outDir);
        Files.createDirectories(dir);
        long now = Instant.now().getEpochSecond() / BAR_SECONDS * BAR_SECONDS; // last closed bar boundary
        long startAll = now - days * 86_400L;
        for (String raw : products) {
            String product = raw.trim();
            status.put(product, "running");
            try {
                status.put(product, backfillProduct(product, dir.resolve(product + ".csv"), startAll, now));
            } catch (Exception e) {
                log.error("CandleBackfillService: {} failed: {}", product, e.getMessage());
                status.put(product, "FAILED: " + e.getMessage());
            }
        }
        status.put("_done", "true");
        log.info("CandleBackfillService: finished — {}", status);
    }

    private String backfillProduct(String product, Path file, long startAll, long now) throws IOException {
        TreeMap<Long, String> rows = new TreeMap<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file)) {
                int c = line.indexOf(',');
                if (c > 0 && Character.isDigit(line.charAt(0))) {
                    rows.put(Long.parseLong(line.substring(0, c)), line);
                }
            }
        }
        long from = rows.isEmpty() ? startAll : Math.max(startAll, rows.lastKey() + BAR_SECONDS);
        long chunk = CANDLES_PER_REQUEST * BAR_SECONDS;
        for (long s = from; s < now; s += chunk) {
            long e = Math.min(s + chunk, now);
            Candles candles = fetchWithRetry(product, s, e);
            if (candles == null) {
                return "missing (404)";
            }
            if (candles.getCandles() == null) {
                continue;
            }
            for (Candle c : candles.getCandles()) {
                if (c.getStart() == null || c.getClose() == null) {
                    continue;
                }
                long ts = c.getStart().getEpochSecond();
                if (ts + BAR_SECONDS > now) {
                    continue; // still forming
                }
                rows.put(ts, ts + "," + p(c.getOpen()) + "," + p(c.getHigh()) + "," + p(c.getLow()) + ","
                        + p(c.getClose()) + "," + p(c.getVolume()));
            }
            status.put(product, "running, " + rows.size() + " bars");
        }
        StringBuilder sb = new StringBuilder("ts,open,high,low,close,volume\n");
        rows.values().forEach(r -> sb.append(r).append('\n'));
        Files.writeString(file, sb.toString());
        return rows.isEmpty() ? "no bars" : "ok, " + rows.size() + " bars from " + Instant.ofEpochSecond(rows.firstKey());
    }

    /** Returns null if the product does not exist (404). */
    private Candles fetchWithRetry(String product, long s, long e) {
        for (int attempt = 1; ; attempt++) {
            try {
                Candles c = productProxy.getPublicCandles(product, Instant.ofEpochSecond(s),
                        Instant.ofEpochSecond(e), Granularity.FIFTEEN_MINUTE);
                pause(PAUSE_MILLIS);
                return c;
            } catch (HttpClientErrorException.NotFound nf) {
                return null;
            } catch (RuntimeException ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
                pause(2_000L * attempt);
            }
        }
    }

    private static String p(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new IOException("interrupted"));
        }
    }
}
