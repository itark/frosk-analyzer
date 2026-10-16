package nu.itark.frosk.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evaluates the five pre-registered VWAP filter hypotheses in {@code ~/itark/PREREG_vwap_signal_filters.md}
 * on the trades written by {@link VwapReplayIT}. Plain JUnit (no Spring context, no DB).
 *
 * <pre>
 *   mvn test -Dtest=VwapFilterAnalysisIT#replayMatchesRecordedSignals   # step 1: gate (§2 of the PREREG)
 *   mvn test -Dtest=VwapFilterAnalysisIT#evaluateHypotheses             # step 2: only after step 1 passes
 * </pre>
 * Inputs (dir = {@code -Dfrosk.replay.dir}, default ~/itark/backfill):
 * {@code replay_trades_exact.csv}, {@code replay_trades_validation.csv}, {@code recorded_vwap_signals.csv}.
 *
 * <p>Everything below the PREREG constants is mechanical. Do not edit thresholds after seeing output;
 * log any change in §9 of the PREREG.
 */
public class VwapFilterAnalysisIT {

    // ---- PREREG constants (§3–§5) -------------------------------------------------------------
    static final long P1_END = Instant.parse("2026-08-01T00:00:00Z").getEpochSecond();
    static final double COST_FEES = 0.20;               // % round trip, taker fees
    static final double COST_FEES_PLUS_EXEC = 0.32;     // % round trip, fees + measured 0.12 execution cost
    static final int BOOTSTRAP_B = 4000;
    static final double MIN_IMPROVEMENT_P1 = 0.05;      // percentage points
    static final double MIN_IMPROVEMENT_P2 = 0.02;
    static final double MAX_HOLM_P = 0.10;
    static final double MIN_KEPT_SHARE = 0.40;
    static final int MIN_KEPT_N = 100;
    static final double MAX_TICKER_SHARE = 0.25;
    static final int CROWD_MIN_PRODUCTS = 5;            // H5

    record Trade(String ticker, long entryTs, double ret, int hour, double stretch,
                 double atrPctile, double relVol, long day, boolean crowded) {}

    record Filter(String name, String description, Predicate<Trade> keep) {}

    // ---- Step 1: replay must reproduce recorded signals --------------------------------------

    // Per-ticker validation rule (PREREG §9, 2026-10-08): the analysis universe is the tickers where
    // the replay demonstrably reproduces the recorded signals. Fixed BEFORE any P&L was inspected.
    static final double TICKER_MIN_RECALL = 0.90;
    static final double TICKER_MIN_PRECISION = 0.85;
    static final int MIN_RECORDED_PER_TICKER = 5;
    static final int MIN_VALIDATED_TICKERS = 10;

    /** Days where the recorded-signal export has known holes (no rows on 09-29, half on 09-30, start-up on 09-08). */
    static final List<String> RECORDING_GAP_DAYS = List.of("2026-09-08", "2026-09-29", "2026-09-30");

    @Test
    public void replayMatchesRecordedSignals() throws IOException {
        Path dir = dir();
        List<Trade> replay = load(dir.resolve("replay_trades_validation.csv"), false);
        // Recorded BUY signals in the window where the live configuration is known (see VwapReplayValidationIT).
        long from = Instant.parse("2026-09-08T00:00:00Z").getEpochSecond();
        long to = Instant.parse("2026-10-05T11:45:00Z").getEpochSecond();
        Map<String, List<Long>> recorded = new HashMap<>();
        for (String line : Files.readAllLines(dir.resolve("recorded_vwap_signals.csv"))) {
            String[] f = line.replace("\"", "").split(",");
            if (f.length < 3 || !f[2].equals("BUY") || !Character.isDigit(f[1].charAt(0))) {
                continue;
            }
            long ts = Long.parseLong(f[1]);
            if (ts >= from && ts <= to && !isGapDay(ts)) {
                recorded.computeIfAbsent(f[0], k -> new ArrayList<>()).add(ts);
            }
        }
        Map<String, List<Long>> replayed = new HashMap<>();
        for (Trade t : replay) {
            if (t.entryTs >= from && t.entryTs <= to && !isGapDay(t.entryTs)) {
                replayed.computeIfAbsent(t.ticker, k -> new ArrayList<>()).add(t.entryTs);
            }
        }
        java.util.Set<String> universe = new java.util.TreeSet<>();
        replay.forEach(t -> universe.add(t.ticker));

        StringBuilder rep = new StringBuilder();
        rep.append(String.format(Locale.ROOT, "%-12s %8s %8s %8s %8s %8s%n",
                "TICKER", "recorded", "found", "recall%", "replayed", "prec%"));
        int recN = 0, recMatched = 0, repN = 0, repMatched = 0;
        int vRecN = 0, vRecMatched = 0, vRepN = 0, vRepMatched = 0;
        java.util.List<String> validated = new ArrayList<>();
        for (String ticker : universe) {
            List<Long> rec = recorded.getOrDefault(ticker, List.of());
            List<Long> rp = replayed.getOrDefault(ticker, List.of());
            int rm = (int) rec.stream().filter(ts -> rp.stream().anyMatch(r -> Math.abs(r - ts) <= 900)).count();
            int pm = (int) rp.stream().filter(ts -> rec.stream().anyMatch(r -> Math.abs(r - ts) <= 900)).count();
            recN += rec.size();
            recMatched += rm;
            repN += rp.size();
            repMatched += pm;
            boolean ok = rec.size() >= MIN_RECORDED_PER_TICKER && rp.size() > 0
                    && (double) rm / rec.size() >= TICKER_MIN_RECALL && (double) pm / rp.size() >= TICKER_MIN_PRECISION;
            if (ok) {
                validated.add(ticker);
                vRecN += rec.size();
                vRecMatched += rm;
                vRepN += rp.size();
                vRepMatched += pm;
            }
            rep.append(String.format(Locale.ROOT, "%-12s %8d %8d %7.1f%% %8d %7.1f%%%n", ticker, rec.size(), rm,
                    rec.isEmpty() ? Double.NaN : 100.0 * rm / rec.size(), rp.size(),
                    rp.isEmpty() ? Double.NaN : 100.0 * pm / rp.size()));
        }
        double recall = recN == 0 ? 0 : (double) recMatched / recN;
        double precision = repN == 0 ? 0 : (double) repMatched / repN;
        double vRecall = vRecN == 0 ? 0 : (double) vRecMatched / vRecN;
        double vPrecision = vRepN == 0 ? 0 : (double) vRepMatched / vRepN;
        rep.append(String.format(Locale.ROOT,
                "%nALL tickers (gap days %s excluded): recorded=%d replayed=%d  recall=%.1f%%  precision=%.1f%%%n",
                RECORDING_GAP_DAYS, recN, repN, 100 * recall, 100 * precision));
        rep.append(String.format(Locale.ROOT,
                "VALIDATED tickers (per ticker recall>=%.0f%% and precision>=%.0f%%, >=%d recorded): %d tickers %s%n"
                        + "  recorded=%d replayed=%d  recall=%.1f%%  precision=%.1f%%%n",
                100 * TICKER_MIN_RECALL, 100 * TICKER_MIN_PRECISION, MIN_RECORDED_PER_TICKER, validated.size(), validated,
                vRecN, vRepN, 100 * vRecall, 100 * vPrecision));
        Files.writeString(dir.resolve("validation_report.txt"), rep);
        Files.writeString(dir.resolve("validated_tickers.txt"), String.join("\n", validated) + "\n");
        System.out.println("\n" + rep);
        assertTrue(validated.size() >= MIN_VALIDATED_TICKERS,
                "Fewer than " + MIN_VALIDATED_TICKERS + " tickers validate — see validation_report.txt");
        assertTrue(vRecall >= 0.90 && vPrecision >= 0.90,
                "Replay does not reproduce the recorded signals even on validated tickers (PREREG §2). "
                        + "See validation_report.txt.");
    }

    private static boolean isGapDay(long ts) {
        return RECORDING_GAP_DAYS.contains(Instant.ofEpochSecond(ts).toString().substring(0, 10));
    }

    // ---- Step 2: the five hypotheses ----------------------------------------------------------

    @Test
    public void evaluateHypotheses() throws IOException {
        Path dir = dir();
        java.util.Set<String> validated = new java.util.HashSet<>(Files.readAllLines(dir.resolve("validated_tickers.txt")));
        validated.removeIf(String::isBlank);
        assertTrue(validated.size() >= MIN_VALIDATED_TICKERS,
                "validated_tickers.txt missing/too short — run replayMatchesRecordedSignals first");
        // crowding (H5) is counted across ALL replayed products, then the analysis set is restricted
        List<Trade> all = load(dir.resolve("replay_trades_exact.csv"), true).stream()
                .filter(t -> validated.contains(t.ticker)).toList();
        List<Trade> p1 = all.stream().filter(t -> t.entryTs < P1_END).toList();
        List<Trade> p2 = all.stream().filter(t -> t.entryTs >= P1_END).toList();

        List<Filter> filters = List.of(
                new Filter("H1 tid 21-22 UTC", "keep hour in {21,22}", t -> t.hour == 21 || t.hour == 22),
                new Filter("H2 hög volatilitet", "exclude ATR% percentile > 67", t -> Double.isNaN(t.atrPctile) || t.atrPctile <= 67.0),
                new Filter("H3 djup stretch", "keep stretch >= 2.0%", t -> !Double.isNaN(t.stretch) && t.stretch >= 2.0),
                new Filter("H4 volymspik", "keep relVol >= 2.0", t -> !Double.isNaN(t.relVol) && t.relVol >= 2.0),
                new Filter("H5 samtidighet", "exclude >=5 products same bar", t -> !t.crowded));

        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "VWAP FILTER EVALUATION (PREREG_vwap_signal_filters.md)%n"));
        out.append(String.format(Locale.ROOT, "Universe: %d validated tickers %s%n", validated.size(), new java.util.TreeSet<>(validated)));
        out.append(String.format(Locale.ROOT, "Trades: P1=%d (%d days)  P2=%d (%d days)%n",
                p1.size(), days(p1), p2.size(), days(p2)));
        out.append(String.format(Locale.ROOT, "Baseline (unfiltered) mean gross/trade: P1=%+.3f%%  P2=%+.3f%%   net@%.2f: P1=%+.3f%% P2=%+.3f%%   net@%.2f: P1=%+.3f%% P2=%+.3f%%%n%n",
                mean(p1), mean(p2), COST_FEES, mean(p1) - COST_FEES, mean(p2) - COST_FEES,
                COST_FEES_PLUS_EXEC, mean(p1) - COST_FEES_PLUS_EXEC, mean(p2) - COST_FEES_PLUS_EXEC));

        double[] pRaw = new double[filters.size()];
        double[] impr1 = new double[filters.size()];
        double[][] ci = new double[filters.size()][];
        for (int i = 0; i < filters.size(); i++) {
            Boot b = bootstrap(p1, filters.get(i).keep, 1000L + i);
            pRaw[i] = b.p;
            impr1[i] = b.diff;
            ci[i] = new double[]{b.lo, b.hi};
        }
        double[] holm = holm(pRaw);

        for (int i = 0; i < filters.size(); i++) {
            Filter f = filters.get(i);
            List<Trade> k1 = p1.stream().filter(f.keep).toList();
            List<Trade> k2 = p2.stream().filter(f.keep).toList();
            double impr2 = k2.isEmpty() ? Double.NaN : mean(k2) - mean(p2);
            double share = p1.isEmpty() ? 0 : (double) k1.size() / p1.size();
            double maxTicker = maxTickerShare(k1);

            List<Trade> sorted = new ArrayList<>(p1);
            sorted.sort(Comparator.comparingLong(Trade::entryTs));
            int mid = sorted.size() / 2;
            List<Trade> h1 = sorted.subList(0, mid);
            List<Trade> h2 = sorted.subList(mid, sorted.size());
            double d1 = halfImprovement(h1, f.keep);
            double d2 = halfImprovement(h2, f.keep);

            boolean c1 = impr1[i] >= MIN_IMPROVEMENT_P1 && !Double.isNaN(impr2) && impr2 >= MIN_IMPROVEMENT_P2;
            boolean c2 = holm[i] < MAX_HOLM_P;
            boolean c3 = share >= MIN_KEPT_SHARE && k1.size() >= MIN_KEPT_N;
            boolean c4 = maxTicker <= MAX_TICKER_SHARE;
            boolean c5 = d1 > 0 && d2 > 0;
            boolean pass = c1 && c2 && c3 && c4 && c5;

            out.append(String.format(Locale.ROOT, "%s  [%s]%n", f.name, f.description));
            out.append(String.format(Locale.ROOT,
                    "  P1: kept %d/%d (%.0f%%)  mean gross kept=%+.3f%% vs all=%+.3f%%  improvement=%+.3f pp  90%%CI[%+.3f, %+.3f]%n",
                    k1.size(), p1.size(), 100 * share, mean(k1), mean(p1), impr1[i], ci[i][0], ci[i][1]));
            out.append(String.format(Locale.ROOT,
                    "      net@%.2f kept=%+.3f%%   net@%.2f kept=%+.3f%%   p=%.4f  Holm p=%.4f  max ticker share=%.0f%%  halves: %+.3f / %+.3f%n",
                    COST_FEES, mean(k1) - COST_FEES, COST_FEES_PLUS_EXEC, mean(k1) - COST_FEES_PLUS_EXEC,
                    pRaw[i], holm[i], 100 * maxTicker, d1, d2));
            out.append(String.format(Locale.ROOT,
                    "  P2: kept %d/%d  improvement=%+.3f pp%n", k2.size(), p2.size(), impr2));
            out.append(String.format(Locale.ROOT,
                    "  Criteria: [1 impr P1>=%.2f & P2>=%.2f]=%s [2 Holm<%.2f]=%s [3 share>=%.0f%% & n>=%d]=%s [4 ticker<=%.0f%%]=%s [5 halves same sign]=%s  => %s%n%n",
                    MIN_IMPROVEMENT_P1, MIN_IMPROVEMENT_P2, yn(c1), MAX_HOLM_P, yn(c2),
                    100 * MIN_KEPT_SHARE, MIN_KEPT_N, yn(c3), 100 * MAX_TICKER_SHARE, yn(c4), yn(c5),
                    pass ? "RECOMMENDED for shadow" : "NOT recommended"));
        }

        Path report = dir.resolve("vwap_filter_report.txt");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(report))) {
            w.print(out);
        }
        System.out.println(out);
        System.out.println("Report: " + report);
    }

    // ---- statistics --------------------------------------------------------------------------

    record Boot(double diff, double lo, double hi, double p) {}

    /** Day-block bootstrap of (mean of kept) − (mean of all); one-sided p for improvement &gt; 0. */
    static Boot bootstrap(List<Trade> trades, Predicate<Trade> keep, long seed) {
        Map<Long, double[]> byDay = new TreeMap<>(); // n, sum, keptN, keptSum
        for (Trade t : trades) {
            double[] a = byDay.computeIfAbsent(t.day, k -> new double[4]);
            a[0]++;
            a[1] += t.ret;
            if (keep.test(t)) {
                a[2]++;
                a[3] += t.ret;
            }
        }
        double[][] days = byDay.values().toArray(new double[0][]);
        double[] tot = new double[4];
        for (double[] d : days) {
            for (int i = 0; i < 4; i++) {
                tot[i] += d[i];
            }
        }
        double observed = tot[2] > 0 ? tot[3] / tot[2] - tot[1] / tot[0] : Double.NaN;
        Random rnd = new Random(seed);
        double[] diffs = new double[BOOTSTRAP_B];
        int valid = 0;
        int leZero = 0;
        for (int b = 0; b < BOOTSTRAP_B; b++) {
            double n = 0, s = 0, kn = 0, ks = 0;
            for (int i = 0; i < days.length; i++) {
                double[] d = days[rnd.nextInt(days.length)];
                n += d[0];
                s += d[1];
                kn += d[2];
                ks += d[3];
            }
            if (kn == 0 || n == 0) {
                continue;
            }
            double diff = ks / kn - s / n;
            diffs[valid++] = diff;
            if (diff <= 0) {
                leZero++;
            }
        }
        if (valid == 0) {
            return new Boot(observed, Double.NaN, Double.NaN, 1.0);
        }
        double[] v = Arrays.copyOf(diffs, valid);
        Arrays.sort(v);
        double p = (leZero + 1.0) / (valid + 1.0);
        return new Boot(observed, v[(int) (0.05 * valid)], v[Math.min(valid - 1, (int) (0.95 * valid))], p);
    }

    static double[] holm(double[] p) {
        int m = p.length;
        Integer[] idx = new Integer[m];
        for (int i = 0; i < m; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, Comparator.comparingDouble(i -> p[i]));
        double[] adj = new double[m];
        double running = 0;
        for (int r = 0; r < m; r++) {
            running = Math.max(running, Math.min(1.0, (m - r) * p[idx[r]]));
            adj[idx[r]] = running;
        }
        return adj;
    }

    static double halfImprovement(List<Trade> half, Predicate<Trade> keep) {
        List<Trade> kept = half.stream().filter(keep).toList();
        return kept.isEmpty() ? Double.NaN : mean(kept) - mean(half);
    }

    static double maxTickerShare(List<Trade> kept) {
        if (kept.isEmpty()) {
            return 1.0;
        }
        Map<String, Integer> c = new HashMap<>();
        kept.forEach(t -> c.merge(t.ticker, 1, Integer::sum));
        return (double) c.values().stream().max(Integer::compare).orElse(0) / kept.size();
    }

    static double mean(List<Trade> ts) {
        return ts.isEmpty() ? Double.NaN : ts.stream().mapToDouble(Trade::ret).average().orElse(Double.NaN);
    }

    static long days(List<Trade> ts) {
        return ts.stream().map(Trade::day).distinct().count();
    }

    static String yn(boolean b) {
        return b ? "yes" : "NO";
    }

    // ---- IO ------------------------------------------------------------------------------------

    static Path dir() {
        String d = System.getProperty("frosk.replay.dir", "~/itark/backfill");
        return Path.of(d.startsWith("~") ? System.getProperty("user.home") + d.substring(1) : d);
    }

    /** Columns: ticker,entryTs,exitTs,entryPrice,exitPrice,retPct,heldBars,hourUtc,stretchPct,atrPct,atrPctile,relVol,rsi7 */
    static List<Trade> load(Path csv, boolean markCrowding) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(csv)) {
            if (line.isEmpty() || line.startsWith("ticker")) {
                continue;
            }
            rows.add(line.split(",", -1));
        }
        Map<Long, java.util.Set<String>> products = new HashMap<>();
        for (String[] f : rows) {
            products.computeIfAbsent(Long.parseLong(f[1]), k -> new java.util.HashSet<>()).add(f[0]);
        }
        List<Trade> out = new ArrayList<>();
        for (String[] f : rows) {
            long ts = Long.parseLong(f[1]);
            out.add(new Trade(f[0], ts, Double.parseDouble(f[5]), Integer.parseInt(f[7]),
                    num(f[8]), num(f[10]), num(f[11]), ts / 86400,
                    markCrowding && products.get(ts).size() >= CROWD_MIN_PRODUCTS));
        }
        return out;
    }


    static double num(String s) {
        return s == null || s.isEmpty() ? Double.NaN : Double.parseDouble(s);
    }
}
