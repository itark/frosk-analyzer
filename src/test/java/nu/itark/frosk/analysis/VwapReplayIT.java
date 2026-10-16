package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.context.TestPropertySource;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Replays the REAL {@code CryptoVWAPReversionIntradayStrategy} (same class, rules and execution
 * model as production) over the 15m CSV history written by {@code CandleBackfillService}, and
 * writes one row per closed trade with entry-time features. Feeds the analysis defined in
 * {@code ~/itark/PREREG_vwap_signal_filters.md}. Nothing is written to any database.
 *
 * <pre>
 *   mvn test -Dtest=VwapReplayIT#replay
 *   # options (system properties):
 *   #   -Dfrosk.replay.dir=~/itark/backfill          input CSVs (ts,open,high,low,close,volume)
 *   #   -Dfrosk.replay.out=~/itark/backfill/replay_trades.csv
 *   #   -Dfrosk.replay.truncateVolume=true           mimic the OLD live behaviour (volume cast to long)
 *   #   -Dfrosk.replay.products=BTC-EUR,SOL-EUR      override universe (default: production universe)
 * </pre>
 *
 * <p>Strategy parameters are pinned below to the values in {@code application-crypto.properties};
 * {@link #productionParametersAreMirrored()} fails if they drift apart, so the replay can never
 * silently test a different strategy than the one running live.
 */
@TestPropertySource(properties = {
        "crypto.vwap.stretch.pct=1.2",
        "crypto.vwap.max.bars.held=24",
        "crypto.vwap.rsi.period=7",
        "crypto.vwap.rsi.entry=35.0",
        "crypto.vwap.cooldown.bars.after.loss=2",
        "crypto.vwap.trend.filter.enabled=true",
        "crypto.vwap.trend.sma.bars=96",
        "crypto.vwap.trend.slope.bars=24",
        "crypto.vwap.lstm.filter.enabled=false"
})
public class VwapReplayIT extends BaseIntegrationTest {

    private static final String STRATEGY = "CryptoVWAPReversionIntradayStrategy";
    private static final long BAR_SECONDS = 900;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final int ATR_PCTILE_LOOKBACK = 30 * 96;

    private static final List<String> PINNED_KEYS = List.of(
            "crypto.vwap.stretch.pct", "crypto.vwap.max.bars.held", "crypto.vwap.rsi.period",
            "crypto.vwap.rsi.entry", "crypto.vwap.cooldown.bars.after.loss",
            "crypto.vwap.trend.filter.enabled", "crypto.vwap.trend.sma.bars", "crypto.vwap.trend.slope.bars");

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    StrategiesMap strategiesMap;

    @Autowired
    Environment env;

    @Test
    public void productionParametersAreMirrored() throws IOException {
        Properties prod = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-crypto.properties"));
        for (String key : PINNED_KEYS) {
            assertEquals(prod.getProperty(key).trim(), env.getProperty(key).trim(),
                    key + " differs between application-crypto.properties and this replay's pinned value");
        }
    }

    @Test
    public void replay() throws IOException {
        Properties prod = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-crypto.properties"));
        Path dir = Path.of(expand(System.getProperty("frosk.replay.dir", "~/itark/backfill")));
        Path out = Path.of(expand(System.getProperty("frosk.replay.out", "~/itark/backfill/replay_trades.csv")));
        boolean truncate = Boolean.parseBoolean(System.getProperty("frosk.replay.truncateVolume", "false"));

        List<String> products = universe(prod);
        System.out.println("\n=== VWAP REPLAY: " + products.size() + " products, truncateVolume=" + truncate + " ===");

        int totalTrades = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out))) {
            w.println("ticker,entryTs,exitTs,entryPrice,exitPrice,retPct,heldBars,hourUtc,stretchPct,"
                    + "atrPct,atrPctile,relVol,rsi7");
            for (String product : products) {
                Path csv = dir.resolve(product + ".csv");
                if (!Files.exists(csv)) {
                    System.out.println(product + ": no CSV — skipped");
                    continue;
                }
                BarSeries series = loadSeries(product, csv, truncate);
                if (series.getBarCount() < 200) {
                    System.out.println(product + ": too few bars (" + series.getBarCount() + ") — skipped");
                    continue;
                }
                Strategy strategy = strategiesMap.getStrategyToRun(STRATEGY, series);
                TradingRecord record = barSeriesService.runConfiguredStrategy(series, strategy);
                int n = writeTrades(w, product, series, record);
                totalTrades += n;
                System.out.println(String.format("%-12s bars=%6d trades=%4d", product, series.getBarCount(), n));
            }
        }
        System.out.println("TOTAL trades: " + totalTrades + " -> " + out);
        assertFalse(totalTrades == 0, "Replay produced no trades — check CSV input and strategy wiring");
    }

    // ---------------------------------------------------------------------------------------

    private List<String> universe(Properties prod) {
        String override = System.getProperty("frosk.replay.products");
        if (override != null && !override.isBlank()) {
            return Arrays.stream(override.split(",")).map(String::trim).toList();
        }
        Set<String> excluded = new HashSet<>();
        for (String key : List.of("crypto.excluded.products", "crypto.vwap.excluded.products")) {
            String raw = prod.getProperty(key, "");
            Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(excluded::add);
        }
        List<String> result = new ArrayList<>();
        for (String p : prod.getProperty("crypto.intraday.products").split(",")) {
            if (!excluded.contains(p.trim())) {
                result.add(p.trim());
            }
        }
        return result;
    }

    private BarSeries loadSeries(String product, Path csv, boolean truncateVolume) throws IOException {
        BarSeries series = new BaseBarSeriesBuilder().withName(product).withNumTypeOf(DoubleNum.class).build();
        long lastTs = Long.MIN_VALUE;
        for (String line : Files.readAllLines(csv)) {
            if (line.isEmpty() || !Character.isDigit(line.charAt(0))) {
                continue;
            }
            String[] f = line.split(",");
            long ts = Long.parseLong(f[0]);
            if (ts <= lastTs) {
                continue;
            }
            lastTs = ts;
            double volume = Double.parseDouble(f[5]);
            if (truncateVolume) {
                volume = (double) (long) volume; // what the old Long column did
            }
            ZonedDateTime end = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), UTC).plus(Duration.ofSeconds(BAR_SECONDS));
            series.addBar(end, Double.parseDouble(f[1]), Double.parseDouble(f[2]),
                    Double.parseDouble(f[3]), Double.parseDouble(f[4]), volume);
        }
        return series;
    }

    private int writeTrades(PrintWriter w, String product, BarSeries series, TradingRecord record) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SessionVWAPIndicator vwap = new SessionVWAPIndicator(series, ZoneOffset.UTC);
        ATRIndicator atr = new ATRIndicator(series, 14);
        RSIIndicator rsi = new RSIIndicator(close, 7);
        int n = 0;
        for (Position pos : record.getPositions()) {
            if (!pos.isClosed()) {
                continue;
            }
            int ei = pos.getEntry().getIndex();
            int xi = pos.getExit().getIndex();
            double entry = pos.getEntry().getPricePerAsset().doubleValue();
            double exit = pos.getExit().getPricePerAsset().doubleValue();
            double ret = (exit / entry - 1.0) * 100.0;

            double c = close.getValue(ei).doubleValue();
            double v = vwap.getValue(ei).doubleValue();
            double stretch = v > 0 ? (v - c) / v * 100.0 : Double.NaN;
            double atrPct = atr.getValue(ei).doubleValue() / c * 100.0;

            // percentile rank of today's ATR% among the previous 30 days of this ticker
            int from = Math.max(0, ei - ATR_PCTILE_LOOKBACK);
            int below = 0;
            int cnt = 0;
            for (int i = from; i < ei; i++) {
                double a = atr.getValue(i).doubleValue() / close.getValue(i).doubleValue() * 100.0;
                if (a < atrPct) {
                    below++;
                }
                cnt++;
            }
            double atrPctile = cnt > 20 ? 100.0 * below / cnt : Double.NaN;

            // volume of the entry bar relative to the median of the previous 20 bars
            double relVol = Double.NaN;
            if (ei >= 20) {
                double[] prev = new double[20];
                for (int i = 0; i < 20; i++) {
                    prev[i] = series.getBar(ei - 20 + i).getVolume().doubleValue();
                }
                Arrays.sort(prev);
                double med = (prev[9] + prev[10]) / 2.0;
                relVol = med > 0 ? series.getBar(ei).getVolume().doubleValue() / med : Double.NaN;
            }

            long entryTs = series.getBar(ei).getEndTime().toEpochSecond() - BAR_SECONDS;
            long exitTs = series.getBar(xi).getEndTime().toEpochSecond() - BAR_SECONDS;
            int hour = ZonedDateTime.ofInstant(Instant.ofEpochSecond(entryTs), UTC).getHour();

            w.println(String.join(",", product, String.valueOf(entryTs), String.valueOf(exitTs),
                    fmt(entry), fmt(exit), fmt(ret), String.valueOf(xi - ei), String.valueOf(hour),
                    fmt(stretch), fmt(atrPct), fmt(atrPctile), fmt(relVol), fmt(rsi.getValue(ei).doubleValue())));
            n++;
        }
        return n;
    }

    private static String fmt(double d) {
        return Double.isNaN(d) ? "" : String.format(java.util.Locale.ROOT, "%.6f", d);
    }

    private static String expand(String p) {
        return p.startsWith("~") ? System.getProperty("user.home") + p.substring(1) : p;
    }
}
