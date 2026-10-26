package nu.itark.frosk.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Track B of {@code ~/itark/PREREG_vwap_signal_filters.md} (§7): what would a maker (limit) entry do to the
 * VWAP strategy's net result? Pure simulation on the 15m CSV bars and the exact-volume replay trades; no Spring.
 *
 * <p>Model (fixed here, before any result was seen):
 * <ul>
 *   <li>For every replayed signal a buy limit is placed at the signal bar's close price (the price the taker
 *       replay entered at) and is live for the NEXT bar only.</li>
 *   <li>Filled if that next bar traded below the limit ({@code low < limit}, conservative: a touch alone does not
 *       fill, since queue position is unknown) or at/below it ({@code low <= limit}, optimistic). A missing bar
 *       (no trades in the interval) is not a fill.</li>
 *   <li>A filled trade keeps the taker replay's exit and gross return (same entry price, same exit bar); unfilled
 *       signals earn 0. Consequently the gross return of the filled subset measures adverse selection directly.</li>
 *   <li>Not modelled: re-entry after an unfilled signal (the strategy would be flat sooner), exit timing relative to
 *       the fill bar, queue priority, partial fills. This is an upper bound on what maker entry can deliver.</li>
 *   <li>Costs, % of notional: taker 0.10 per leg; maker 0.02 per leg. Scenarios: taker/taker 0.20 (baseline),
 *       maker entry + taker exit 0.12, maker both legs 0.04 (needs a resting exit order: not modelled, bound only).</li>
 * </ul>
 * Run after {@code VwapFilterAnalysisIT#replayMatchesRecordedSignals}:
 * {@code mvn test -Dmaven.test.skip=false -Dtest=VwapMakerEntryIT}. Output: {@code maker_report.txt}.
 */
public class VwapMakerEntryIT {

    static final long P1_END = Instant.parse("2026-08-01T00:00:00Z").getEpochSecond();
    static final long BAR = 900;
    static final double TAKER_LEG = 0.10;
    static final double MAKER_LEG = 0.02;
    static final int B = 4000;

    record Tr(String ticker, long ts, double ret, long day, boolean strict, boolean optimistic) {}

    @Test
    public void makerEntryReport() throws IOException {
        Path dir = VwapFilterAnalysisIT.dir();
        Set<String> validated = new HashSet<>(Files.readAllLines(dir.resolve("validated_tickers.txt")));
        validated.removeIf(String::isBlank);
        assertTrue(validated.size() >= 10, "run VwapFilterAnalysisIT#replayMatchesRecordedSignals first");

        Map<String, Map<Long, double[]>> bars = new HashMap<>();
        List<Tr> trades = new ArrayList<>();
        int noNextBar = 0;
        for (String line : Files.readAllLines(dir.resolve("replay_trades_exact.csv"))) {
            if (line.isEmpty() || line.startsWith("ticker")) {
                continue;
            }
            String[] f = line.split(",", -1);
            if (!validated.contains(f[0])) {
                continue;
            }
            long ts = Long.parseLong(f[1]);
            double limit = Double.parseDouble(f[3]);
            Map<Long, double[]> series = bars.computeIfAbsent(f[0], t -> loadLows(dir.resolve(t + ".csv")));
            double[] next = series.get(ts + BAR);   // {low}
            boolean strict = false;
            boolean opt = false;
            if (next == null) {
                noNextBar++;
            } else {
                strict = next[0] < limit;
                opt = next[0] <= limit;
            }
            trades.add(new Tr(f[0], ts, Double.parseDouble(f[5]), ts / 86400, strict, opt));
        }

        StringBuilder out = new StringBuilder();
        out.append("TRACK B: maker (limit) entry at signal-bar close, live for the next 15m bar (PREREG §7)\n");
        out.append(String.format(Locale.ROOT, "Universe: %d validated tickers. Signals with no trades in the next bar: %d%n%n",
                validated.size(), noNextBar));

        for (String period : List.of("P1", "P2")) {
            List<Tr> p = trades.stream().filter(t -> period.equals("P1") ? t.ts < P1_END : t.ts >= P1_END).toList();
            double base = mean(p) - 2 * TAKER_LEG;
            out.append(String.format(Locale.ROOT, "%s: %d signals (%d days). Baseline taker/taker: gross %+.3f%%  net %+.3f%% per trade%n",
                    period, p.size(), p.stream().map(Tr::day).distinct().count(), mean(p), base));
            for (boolean strict : new boolean[]{true, false}) {
                List<Tr> filled = p.stream().filter(t -> strict ? t.strict : t.optimistic).toList();
                List<Tr> missed = p.stream().filter(t -> !(strict ? t.strict : t.optimistic)).toList();
                out.append(String.format(Locale.ROOT,
                        "  fill rule %-26s fill rate %5.1f%%  gross filled %+.3f%%  gross missed %+.3f%%  (adverse selection = %+.3f pp)%n",
                        strict ? "low < limit (conservative)" : "low <= limit (optimistic)",
                        100.0 * filled.size() / p.size(), mean(filled), mean(missed), mean(filled) - mean(p)));
                for (double[] sc : new double[][]{{MAKER_LEG + TAKER_LEG, 0}, {2 * MAKER_LEG, 1}}) {
                    double cost = sc[0];
                    double perFilled = mean(filled) - cost;
                    double perSignal = (filled.size() * perFilled) / p.size();
                    Boot bt = bootstrap(p, strict, cost, 7L + (strict ? 1 : 2) + (sc[1] == 0 ? 10 : 20));
                    out.append(String.format(Locale.ROOT,
                            "    cost %.2f%% (%s): net per filled %+.3f%%  net per SIGNAL %+.3f%%  vs baseline %+.3f%%  diff %+.3f pp  90%%CI[%+.3f, %+.3f]  p(diff<=0)=%.4f%n",
                            cost, sc[1] == 0 ? "maker entry + taker exit" : "maker both legs, bound only",
                            perFilled, perSignal, base, perSignal - base, bt.lo, bt.hi, bt.p));
                }
            }
            out.append('\n');
        }
        out.append("Reading guide: 'net per SIGNAL' counts unfilled signals as 0 and is what compares to the taker baseline.\n"
                + "A positive diff is an upper bound: re-entry after missed fills, queue position and resting exits are not modelled.\n");
        Files.writeString(dir.resolve("maker_report.txt"), out);
        System.out.println("\n" + out);
    }

    // ---------------------------------------------------------------------------------------

    record Boot(double lo, double hi, double p) {}

    /** Day-block bootstrap of (maker net per signal) − (taker net per signal). */
    static Boot bootstrap(List<Tr> trades, boolean strict, double makerCost, long seed) {
        Map<Long, double[]> byDay = new TreeMap<>(); // n, sumRet, sumNetMakerFilled
        for (Tr t : trades) {
            double[] a = byDay.computeIfAbsent(t.day, k -> new double[3]);
            a[0]++;
            a[1] += t.ret;
            if (strict ? t.strict : t.optimistic) {
                a[2] += t.ret - makerCost;
            }
        }
        double[][] days = byDay.values().toArray(new double[0][]);
        Random rnd = new Random(seed);
        double[] diffs = new double[B];
        int le = 0;
        for (int b = 0; b < B; b++) {
            double n = 0, s = 0, m = 0;
            for (int i = 0; i < days.length; i++) {
                double[] d = days[rnd.nextInt(days.length)];
                n += d[0];
                s += d[1];
                m += d[2];
            }
            double diff = m / n - (s / n - 2 * TAKER_LEG);
            diffs[b] = diff;
            if (diff <= 0) {
                le++;
            }
        }
        Arrays.sort(diffs);
        return new Boot(diffs[(int) (0.05 * B)], diffs[(int) (0.95 * B)], (le + 1.0) / (B + 1.0));
    }

    static double mean(List<Tr> ts) {
        return ts.isEmpty() ? Double.NaN : ts.stream().mapToDouble(Tr::ret).average().orElse(Double.NaN);
    }

    /** ts -> {low} for one ticker's bar CSV (ts,open,high,low,close,volume). */
    static Map<Long, double[]> loadLows(Path csv) {
        Map<Long, double[]> m = new HashMap<>();
        try {
            for (String line : Files.readAllLines(csv)) {
                if (line.isEmpty() || !Character.isDigit(line.charAt(0))) {
                    continue;
                }
                String[] f = line.split(",");
                m.put(Long.parseLong(f[0]), new double[]{Double.parseDouble(f[3])});
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return m;
    }
}
