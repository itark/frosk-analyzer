package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.BundleKopCandidateDTO;
import nu.itark.frosk.analysis.BundleKopScreeningDTO;
import nu.itark.frosk.analysis.MarketRegimeDTO;
import nu.itark.frosk.analysis.MomentumLowVolCandidateDTO;
import nu.itark.frosk.analysis.MomentumLowVolScreeningDTO;
import nu.itark.frosk.analysis.MultiFactorCandidateDTO;
import nu.itark.frosk.analysis.MultiFactorScreeningDTO;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cross-sectional equity factor screens — S1 and S2 in
 * {@code docs/research/omx-equity-strategies-research.md}.
 *
 * <p>Both rank the whole active YAHOO universe at the current rebalance point (the doc
 * specifies screening is done "externally in Java per rebalance day", not per-series ta4j).
 * Read-only aggregation — no orders, not registered as live portfolio strategies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EquityFactorScreeningService {

    private final BarSeriesService barSeriesService;
    private final SecurityRepository securityRepository;
    private final MarketTimingService marketTimingService;

    // ── S1: Momentum-first low-vol double screening ──────────────────────────
    @Value("${frosk.s1.momentum.lookback.bars:252}")
    private int s1MomentumLookback;
    @Value("${frosk.s1.vol.lookback.bars:20}")
    private int s1VolLookback;
    /** Top fraction of the universe by momentum that survives the first screen (tertile = 0.33). */
    @Value("${frosk.s1.momentum.top.fraction:0.33}")
    private double s1TopFraction;
    @Value("${frosk.s1.max.positions:25}")
    private int s1MaxPositions;

    // ── S2: Multi-factor composite z-score ───────────────────────────────────
    @Value("${frosk.s2.momentum.lookback.bars:252}")
    private int s2MomentumLookback;
    @Value("${frosk.s2.weight.momentum:0.4}")
    private double s2WeightMomentum;
    @Value("${frosk.s2.weight.value:0.3}")
    private double s2WeightValue;
    @Value("${frosk.s2.weight.quality:0.3}")
    private double s2WeightQuality;
    @Value("${frosk.s2.zscore.cap:3.0}")
    private double s2ZCap;
    @Value("${frosk.s2.max.positions:20}")
    private int s2MaxPositions;

    // ── S6: BundleKöp 4-factor ensemble (momentum/value/quality/low-vol), market-gated ──
    @Value("${frosk.s6.momentum.lookback.bars:252}")
    private int s6MomentumLookback;
    @Value("${frosk.s6.vol.lookback.bars:20}")
    private int s6VolLookback;
    @Value("${frosk.s6.weight.momentum:0.3}")
    private double s6WeightMomentum;
    @Value("${frosk.s6.weight.value:0.2}")
    private double s6WeightValue;
    @Value("${frosk.s6.weight.quality:0.2}")
    private double s6WeightQuality;
    @Value("${frosk.s6.weight.lowvol:0.3}")
    private double s6WeightLowVol;
    @Value("${frosk.s6.zscore.cap:3.0}")
    private double s6ZCap;
    @Value("${frosk.s6.max.positions:15}")
    private int s6MaxPositions;

    /**
     * Active YAHOO universe keyed by security id (as string). {@code getDataSet(Database)} names
     * each BarSeries by {@code security.getId()}, so the screens join price series to fundamentals
     * on that id — not the ticker.
     */
    private Map<String, Security> activeUniverseById() {
        return securityRepository.findByDatabaseAndActive("YAHOO", true).stream()
                .collect(Collectors.toMap(s -> String.valueOf(s.getId()), Function.identity(), (a, b) -> a));
    }

    // ───────────────────────────── S1 ───────────────────────────────────────

    public MomentumLowVolScreeningDTO screenMomentumLowVol() {
        Map<String, Security> byId = activeUniverseById();
        List<MomentumLowVolCandidateDTO> rows = new ArrayList<>();
        for (BarSeries series : barSeriesService.getDataSet(Database.YAHOO)) {
            if (series.getBarCount() <= s1MomentumLookback + 1) {
                continue;
            }
            double momentum = momentum(series, s1MomentumLookback);
            double vol = realizedVol(series, s1VolLookback);
            if (Double.isNaN(momentum) || Double.isNaN(vol)) {
                continue;
            }
            Security sec = byId.get(series.getName());
            rows.add(MomentumLowVolCandidateDTO.builder()
                    .securityName(sec != null ? sec.getName() : series.getName())
                    .securityDesc(sec != null ? sec.getDescription() : null)
                    .momentum12mPct(round(momentum))
                    .volatility(round(vol * 100.0))      // daily log-return stdev, in %
                    .selected(false)
                    .build());
        }

        int universeSize = rows.size();
        double medianVol = median(rows.stream().mapToDouble(MomentumLowVolCandidateDTO::getVolatility).toArray());
        // Momentum-first: keep the top fraction by momentum, then the low-vol half of those.
        List<MomentumLowVolCandidateDTO> byMomentum = rows.stream()
                .sorted(Comparator.comparingDouble(MomentumLowVolCandidateDTO::getMomentum12mPct).reversed())
                .collect(Collectors.toList());
        int tertileCount = Math.max(1, (int) Math.round(universeSize * s1TopFraction));
        double momentumCutoff = universeSize == 0 ? 0 : byMomentum.get(Math.min(tertileCount, universeSize) - 1).getMomentum12mPct();

        List<MomentumLowVolCandidateDTO> selected = new ArrayList<>();
        for (int i = 0; i < byMomentum.size() && i < tertileCount; i++) {
            MomentumLowVolCandidateDTO c = byMomentum.get(i);
            if (c.getVolatility() < medianVol) {
                c.setSelected(true);
                selected.add(c);
            }
        }
        if (selected.size() > s1MaxPositions) {
            selected = selected.subList(0, s1MaxPositions);
        }

        log.info("S1 momentum-lowvol: universe={}, momentum cutoff={}, medianVol={}, selected={}",
                universeSize, round(momentumCutoff), round(medianVol), selected.size());

        return MomentumLowVolScreeningDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .universeSize(universeSize)
                .momentumCutoffPct(round(momentumCutoff))
                .medianVolatility(round(medianVol))
                .maxPositions(s1MaxPositions)
                .selectedCount(selected.size())
                .selected(selected)
                .build();
    }

    // ───────────────────────────── S2 ───────────────────────────────────────

    public MultiFactorScreeningDTO screenMultiFactorComposite() {
        Map<String, Security> byId = activeUniverseById();

        // Raw factor rows (only securities where all three factors are computable — intersection).
        List<MultiFactorCandidateDTO> rows = new ArrayList<>();
        List<Double> moms = new ArrayList<>();
        List<Double> vals = new ArrayList<>();
        List<Double> quals = new ArrayList<>();
        for (BarSeries series : barSeriesService.getDataSet(Database.YAHOO)) {
            if (series.getBarCount() <= s2MomentumLookback + 1) {
                continue;
            }
            Security sec = byId.get(series.getName());
            if (sec == null) {
                continue;
            }
            double momentum = momentum(series, s2MomentumLookback);
            if (Double.isNaN(momentum)) {
                continue;
            }
            double lastClose = series.getBar(series.getEndIndex()).getClosePrice().doubleValue();
            double[] value = valueFactor(sec, lastClose); // [raw, sourceFlag] 1=P/B, 2=earningsYield
            double[] quality = qualityFactor(sec); // [raw, sourceFlag] where sourceFlag 1=debt/eq, 2=beta
            if (value == null || quality == null) {
                continue; // can't value or can't assess quality -> excluded from the intersection
            }
            rows.add(MultiFactorCandidateDTO.builder()
                    .securityName(sec.getName())
                    .securityDesc(sec.getDescription())
                    .momentum12mPct(round(momentum))
                    .valueRaw(round(value[0]))
                    .valueSource(value[1] == 1 ? "priceToBook" : "earningsYield")
                    .qualityRaw(round(quality[0]))
                    .qualitySource(quality[1] == 1 ? "debtToEquity" : "beta")
                    .build());
            moms.add(momentum);
            vals.add(value[0]);
            quals.add(quality[0]);
        }

        int universeSize = rows.size();
        double[] zMom = zscores(toArray(moms), s2ZCap);
        double[] zVal = zscores(toArray(vals), s2ZCap);
        double[] zQual = zscores(toArray(quals), s2ZCap);
        for (int i = 0; i < rows.size(); i++) {
            MultiFactorCandidateDTO c = rows.get(i);
            c.setZMomentum(round(zMom[i]));
            c.setZValue(round(zVal[i]));
            c.setZQuality(round(zQual[i]));
            c.setComposite(round(s2WeightMomentum * zMom[i] + s2WeightValue * zVal[i] + s2WeightQuality * zQual[i]));
        }

        // Intersection requirement: composite > 0; then top-N by composite.
        List<MultiFactorCandidateDTO> selected = rows.stream()
                .filter(c -> c.getComposite() > 0)
                .sorted(Comparator.comparingDouble(MultiFactorCandidateDTO::getComposite).reversed())
                .limit(s2MaxPositions)
                .collect(Collectors.toList());

        log.info("S2 multi-factor: universe={}, weights m/v/q={}/{}/{}, selected={}",
                universeSize, s2WeightMomentum, s2WeightValue, s2WeightQuality, selected.size());

        return MultiFactorScreeningDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .universeSize(universeSize)
                .weightMomentum(s2WeightMomentum)
                .weightValue(s2WeightValue)
                .weightQuality(s2WeightQuality)
                .maxPositions(s2MaxPositions)
                .selectedCount(selected.size())
                .selected(selected)
                .build();
    }

    // ───────────────────────────── S6 ───────────────────────────────────────

    /**
     * S6 BundleKöp: integrated 4-factor composite (momentum, value, quality, low-vol), gated by
     * the S4 market regime. No new buys unless TSMOM and SMA-200 are both bullish.
     */
    public BundleKopScreeningDTO screenBundleKop() {
        MarketRegimeDTO regime = marketTimingService.marketRegime();
        boolean marketBull = regime.isTsmomBull() && regime.isSma200Bull();
        Map<String, Security> byId = activeUniverseById();

        List<BundleKopCandidateDTO> rows = new ArrayList<>();
        List<Double> moms = new ArrayList<>();
        List<Double> vals = new ArrayList<>();
        List<Double> quals = new ArrayList<>();
        List<Double> lowvols = new ArrayList<>();
        for (BarSeries series : barSeriesService.getDataSet(Database.YAHOO)) {
            if (series.getBarCount() <= s6MomentumLookback + 1) {
                continue;
            }
            Security sec = byId.get(series.getName());
            if (sec == null) {
                continue;
            }
            double momentum = momentum(series, s6MomentumLookback);
            double vol = realizedVol(series, s6VolLookback);
            if (Double.isNaN(momentum) || Double.isNaN(vol)) {
                continue;
            }
            double lastClose = series.getBar(series.getEndIndex()).getClosePrice().doubleValue();
            double[] value = valueFactor(sec, lastClose);
            double[] quality = qualityFactor(sec);
            if (value == null || quality == null) {
                continue;
            }
            rows.add(BundleKopCandidateDTO.builder()
                    .securityName(sec.getName())
                    .securityDesc(sec.getDescription())
                    .momentum12mPct(round(momentum))
                    .valueRaw(round(value[0]))
                    .qualityRaw(round(quality[0]))
                    .volatility(round(vol * 100.0))
                    .build());
            moms.add(momentum);
            vals.add(value[0]);
            quals.add(quality[0]);
            lowvols.add(-vol); // low vol -> high score
        }

        int universeSize = rows.size();
        double[] zMom = zscores(toArray(moms), s6ZCap);
        double[] zVal = zscores(toArray(vals), s6ZCap);
        double[] zQual = zscores(toArray(quals), s6ZCap);
        double[] zLow = zscores(toArray(lowvols), s6ZCap);
        for (int i = 0; i < rows.size(); i++) {
            BundleKopCandidateDTO c = rows.get(i);
            c.setZMomentum(round(zMom[i]));
            c.setZValue(round(zVal[i]));
            c.setZQuality(round(zQual[i]));
            c.setZLowVol(round(zLow[i]));
            c.setComposite(round(s6WeightMomentum * zMom[i] + s6WeightValue * zVal[i]
                    + s6WeightQuality * zQual[i] + s6WeightLowVol * zLow[i]));
        }

        // Market-regime gate: no new buys unless both trend signals are bullish.
        List<BundleKopCandidateDTO> selected = marketBull
                ? rows.stream()
                    .filter(c -> c.getComposite() > 0)
                    .sorted(Comparator.comparingDouble(BundleKopCandidateDTO::getComposite).reversed())
                    .limit(s6MaxPositions)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        log.info("S6 BundleKop: universe={}, marketBull={}, selected={}", universeSize, marketBull, selected.size());

        return BundleKopScreeningDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .universeSize(universeSize)
                .marketBull(marketBull)
                .marketRegime(regime.getRegime())
                .weightMomentum(s6WeightMomentum)
                .weightValue(s6WeightValue)
                .weightQuality(s6WeightQuality)
                .weightLowVol(s6WeightLowVol)
                .maxPositions(s6MaxPositions)
                .selectedCount(selected.size())
                .selected(selected)
                .build();
    }

    /**
     * @return [rawValueFactor (higher=cheaper), source] or null if value can't be assessed.
     * Source 1 = 1/priceToBook (spec). Source 2 = trailing earnings yield (EPS/price) — the
     * fallback uses EPS, not trailingPe, because the Yahoo sync does not populate trailingPe
     * for this universe while EPS has the widest coverage.
     */
    private static double[] valueFactor(Security s, double lastClose) {
        if (s.getPriceToBook() != null && s.getPriceToBook() > 0) {
            return new double[]{1.0 / s.getPriceToBook(), 1};
        }
        if (s.getTrailingEps() != null && s.getTrailingEps() > 0 && lastClose > 0) {
            return new double[]{s.getTrailingEps() / lastClose, 2};
        }
        return null;
    }

    /** @return [rawQualityFactor (higher=better), source] (1=-debtToEquity, 2=-beta) or null. */
    private static double[] qualityFactor(Security s) {
        if (s.getDebtToEquity() != null && s.getDebtToEquity() > 0) {
            return new double[]{-s.getDebtToEquity(), 1};
        }
        if (s.getBeta() != null && s.getBeta() > 0) {
            return new double[]{-s.getBeta(), 2};
        }
        return null;
    }

    // ───────────────────────── pure, testable helpers ───────────────────────

    /** 12-month return in percent via ta4j ROC over {@code lookback} bars at the last bar. */
    static double momentum(BarSeries series, int lookback) {
        ROCIndicator roc = new ROCIndicator(new ClosePriceIndicator(series), lookback);
        return roc.getValue(series.getEndIndex()).doubleValue();
    }

    /** Stdev of daily log returns over the last {@code lookback} bars (fraction, not %). */
    static double realizedVol(BarSeries series, int lookback) {
        int end = series.getEndIndex();
        int firstPrice = end - lookback; // need lookback returns -> lookback+1 prices
        if (firstPrice < series.getBeginIndex()) {
            return Double.NaN;
        }
        double[] rets = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            double c0 = series.getBar(firstPrice + i).getClosePrice().doubleValue();
            double c1 = series.getBar(firstPrice + i + 1).getClosePrice().doubleValue();
            rets[i] = (c0 > 0 && c1 > 0) ? Math.log(c1 / c0) : 0.0;
        }
        return std(rets);
    }

    /** Cross-sectional z-scores, clamped to ±cap. Returns all-zero if stdev is 0. */
    static double[] zscores(double[] x, double cap) {
        double[] z = new double[x.length];
        if (x.length == 0) {
            return z;
        }
        double m = mean(x);
        double sd = std(x);
        for (int i = 0; i < x.length; i++) {
            z[i] = sd > 0 ? Math.max(-cap, Math.min(cap, (x[i] - m) / sd)) : 0.0;
        }
        return z;
    }

    static double mean(double[] x) {
        if (x.length == 0) return 0;
        double s = 0;
        for (double v : x) s += v;
        return s / x.length;
    }

    static double std(double[] x) {
        if (x.length == 0) return 0;
        double m = mean(x);
        double s = 0;
        for (double v : x) s += (v - m) * (v - m);
        return Math.sqrt(s / x.length);
    }

    static double median(double[] x) {
        if (x.length == 0) return 0;
        double[] c = x.clone();
        Arrays.sort(c);
        int n = c.length;
        return (n % 2 == 1) ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2.0;
    }

    private static double[] toArray(List<Double> list) {
        double[] a = new double[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
