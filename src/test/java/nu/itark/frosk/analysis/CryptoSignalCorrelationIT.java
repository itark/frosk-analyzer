package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.CryptoIntradayDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * <b>Brainstorm #6, step 6a — ensemble feasibility measurement.</b>
 *
 * <p>Before building any 2-of-3 voting meta-strategy, this measures whether the
 * candidate strategies' signal outputs are actually decorrelated — the research
 * gate is pairwise correlation &lt; {@value #GATE}. If two strategies are in the
 * market at the same times, a vote between them adds no information.
 *
 * <p>Method (deterministic, not dependent on accumulated live {@code
 * intraday_signal} rows, which the new strategies have barely started to
 * produce): run each strategy over the same crypto 15m series through the
 * production execution path, derive a per-bar in-position vector (1 while a
 * position is held), pool those vectors across all (series, bar), and compute
 * the pairwise Pearson correlation. Zero-variance strategies (too few signals
 * on the sampled data) are reported as {@code n/a} rather than a spurious
 * correlation.
 *
 * <p>Usage (needs the crypto profile for the Coinbase data service + network):
 * <pre>
 *   mvn test -Dmaven.test.skip=false -Dtest=CryptoSignalCorrelationIT#signalCorrelation \
 *     -Dspring.profiles.include=crypto \
 *     -Dcrypto.intraday.products=BTC-EUR,ETH-EUR,SOL-EUR,XRP-EUR,ADA-EUR,LTC-EUR,BCH-EUR,ATOM-EUR,XLM-EUR,XTZ-EUR
 *   # optional: -Dfrosk.compare.strategies=Name1,Name2,...
 * </pre>
 * Writes nothing but the synced bars; the correlation report goes to stdout.
 */
public class CryptoSignalCorrelationIT extends BaseIntegrationTest {

    /** Research gate: a pair must be below this to be worth ensembling. */
    private static final double GATE = 0.4;

    private static final int MIN_BARS = 200;

    private static final List<String> DEFAULT_STRATEGIES = List.of(
            "CryptoVWAPReversionIntradayStrategy",
            "CryptoLiquiditySweepIntradayStrategy",
            "CryptoNightReversionIntradayStrategy",
            "CryptoMomentumSpilloverIntradayStrategy",
            "CryptoEmaAdxTrendIntradayStrategy",
            "CryptoBollingerSqueezeIntradayStrategy"
    );

    @Value("${crypto.intraday.products}")
    private List<String> products;

    @Autowired
    private BarSeriesService barSeriesService;

    @Autowired
    private StrategiesMap strategiesMap;

    @Autowired
    private CryptoIntradayDataService cryptoIntradayDataService;

    @Autowired
    private nu.itark.frosk.repo.SecurityRepository securityRepository;

    @Test
    public void signalCorrelation() {
        seedProductsIfMissing();
        Map<Security, BarSeries> seriesMap = cryptoIntradayDataService.syncAndBuildAllSeries();
        assertFalse(seriesMap.isEmpty(), "No crypto intraday series — Coinbase sync failed?");

        List<String> strategies = configuredStrategies();

        // Pooled per-bar in-position vectors, index-aligned across strategies.
        Map<String, List<Double>> vectors = new LinkedHashMap<>();
        strategies.forEach(s -> vectors.put(s, new ArrayList<>()));

        int usedSeries = 0;
        for (BarSeries series : seriesMap.values()) {
            if (series.getBarCount() < MIN_BARS) {
                continue;
            }
            Map<String, double[]> perStrategy = new LinkedHashMap<>();
            for (String strategyName : strategies) {
                perStrategy.put(strategyName, inPositionVector(strategyName, series));
            }
            int n = series.getBarCount();
            for (int i = 0; i < n; i++) {
                for (String strategyName : strategies) {
                    vectors.get(strategyName).add(perStrategy.get(strategyName)[i]);
                }
            }
            usedSeries++;
        }

        printReport(strategies, vectors, usedSeries);
    }

    /** 1.0 while a position is held (entry..exit inclusive), else 0.0; errors → all-zero vector. */
    private double[] inPositionVector(String strategyName, BarSeries series) {
        double[] inPos = new double[series.getBarCount()];
        try {
            Strategy strategy = strategiesMap.getStrategyToRun(strategyName, series);
            TradingRecord record = barSeriesService.runConfiguredStrategy(series, strategy);
            int endIndex = series.getEndIndex();
            for (Position position : record.getPositions()) {
                int entry = position.getEntry().getIndex();
                int exit = position.getExit() != null ? position.getExit().getIndex() : endIndex;
                for (int i = entry; i <= exit && i < inPos.length; i++) {
                    inPos[i] = 1.0;
                }
            }
        } catch (Exception e) {
            System.out.println("  (correlation) " + strategyName + " failed on "
                    + series.getName() + ": " + e.getMessage());
        }
        return inPos;
    }

    private void printReport(List<String> strategies, Map<String, List<Double>> vectors, int usedSeries) {
        System.out.println("\n=== CRYPTO SIGNAL CORRELATION (" + usedSeries + " series, in-position per bar) ===");

        // Per-strategy bars-in-market share, so we can tell which have enough signal to trust.
        System.out.println("\nSignal presence (share of bars in a position):");
        for (String s : strategies) {
            double[] v = toArray(vectors.get(s));
            double share = v.length == 0 ? 0 : sum(v) / v.length;
            System.out.printf("  %-42s %.4f  (%d bars in market)%n", s, share, (long) sum(v));
        }

        System.out.println("\nPairwise Pearson correlation (gate: < " + GATE + " to ensemble):");
        String header = String.format("%-42s", "") +
                String.join("", strategies.stream().map(s -> String.format("%12s", abbrev(s))).toList());
        System.out.println(header);
        for (String a : strategies) {
            StringBuilder row = new StringBuilder(String.format("%-42s", a));
            for (String b : strategies) {
                double c = pearson(toArray(vectors.get(a)), toArray(vectors.get(b)));
                row.append(Double.isNaN(c) ? String.format("%12s", "n/a") : String.format("%12.3f", c));
            }
            System.out.println(row);
        }

        System.out.println("\nDecorrelated pairs (< " + GATE + ", both strategies with signal):");
        boolean any = false;
        for (int i = 0; i < strategies.size(); i++) {
            for (int j = i + 1; j < strategies.size(); j++) {
                double c = pearson(toArray(vectors.get(strategies.get(i))), toArray(vectors.get(strategies.get(j))));
                if (!Double.isNaN(c) && c < GATE) {
                    System.out.printf("  %s  ⟂  %s   (corr=%.3f)%n", strategies.get(i), strategies.get(j), c);
                    any = true;
                }
            }
        }
        if (!any) {
            System.out.println("  (none — either all pairs correlate >= gate, or too few signals to measure)");
        }
        System.out.println("\nNOTE: n/a = zero variance (too few signals on the sampled data — needs the full "
                + "universe and/or more forward data before the gate is trustworthy).\n");
    }

    private List<String> configuredStrategies() {
        String override = System.getProperty("frosk.compare.strategies");
        if (override != null && !override.isBlank()) {
            return Arrays.stream(override.split(",")).map(String::trim).toList();
        }
        return DEFAULT_STRATEGIES;
    }

    private void seedProductsIfMissing() {
        for (String product : products) {
            String p = product.trim();
            if (p.isEmpty() || securityRepository.findByName(p) != null) {
                continue;
            }
            String currency = p.contains("-") ? p.substring(p.lastIndexOf('-') + 1) : "EUR";
            Security seed = new Security(p, p + " (corr seed)", "COINBASE", currency);
            seed.setActive(true);
            securityRepository.save(seed);
        }
    }

    private static String abbrev(String strategyName) {
        String s = strategyName.replace("Crypto", "").replace("IntradayStrategy", "");
        return s.length() > 11 ? s.substring(0, 11) : s;
    }

    private static double[] toArray(List<Double> list) {
        double[] a = new double[list.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    private static double sum(double[] v) {
        double s = 0;
        for (double x : v) {
            s += x;
        }
        return s;
    }

    /** Pearson correlation; NaN if either series has zero variance. */
    private static double pearson(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n == 0) {
            return Double.NaN;
        }
        double mx = 0, my = 0;
        for (int i = 0; i < n; i++) {
            mx += x[i];
            my += y[i];
        }
        mx /= n;
        my /= n;
        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - mx, dy = y[i] - my;
            sxy += dx * dy;
            sxx += dx * dx;
            syy += dy * dy;
        }
        if (sxx == 0 || syy == 0) {
            return Double.NaN;
        }
        return sxy / Math.sqrt(sxx * syy);
    }
}
