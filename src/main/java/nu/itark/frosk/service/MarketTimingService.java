package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.MarketRegimeDTO;
import nu.itark.frosk.analysis.NordicCalendarDTO;
import nu.itark.frosk.analysis.SeasonalTimingDTO;
import nu.itark.frosk.analysis.TurnOfMonthDTO;
import nu.itark.frosk.util.TradingCalendar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Seasonal / market-regime / calendar timing overlays — S3, S4, S5, S8 in
 * {@code docs/research/omx-equity-strategies-research.md}. All read-only point-in-time signals
 * (as of today); they place no orders and gate nothing automatically.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketTimingService {

    private final BarSeriesService barSeriesService;

    @Value("${frosk.timing.index:^OMX}")
    private String indexTicker;
    @Value("${frosk.timing.tsmom.lookback.bars:252}")
    private int tsmomLookback;
    @Value("${frosk.timing.sma.lookback.bars:200}")
    private int smaLookback;
    @Value("${frosk.timing.sma.buffer.pct:2.0}")
    private double smaBufferPct;
    @Value("${frosk.timing.vol.lookback.bars:20}")
    private int volLookback;
    @Value("${frosk.timing.vol.median.bars:252}")
    private int volMedianBars;
    @Value("${frosk.timing.vol.elevated.mult:1.5}")
    private double volElevatedMult;

    private BarSeries indexSeries() {
        try {
            return barSeriesService.getDataSet(indexTicker, false, false);
        } catch (Exception e) {
            log.warn("MarketTimingService: could not load index {} — {}", indexTicker, e.toString());
            return null;
        }
    }

    /** S3 — Halloween seasonal overlay with OMX momentum confirmation. */
    public SeasonalTimingDTO seasonal() {
        LocalDate today = LocalDate.now();
        boolean season = TradingCalendar.isHalloweenSeason(today);
        double momentum = indexMomentum();
        boolean momentumPositive = momentum > 0;
        boolean invested = season && momentumPositive;
        String guidance = invested
                ? "In season (Nov–Apr) and OMX momentum positive — invested."
                : season ? "In season but OMX 12m momentum is not positive — stay defensive."
                         : "Out of season (May–Oct) — defensive / cash.";
        return SeasonalTimingDTO.builder()
                .date(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .halloweenSeason(season)
                .omxMomentum12mPct(round(momentum))
                .momentumPositive(momentumPositive)
                .invested(invested)
                .guidance(guidance)
                .build();
    }

    /** S4 — TSMOM + SMA-200 two-layer market filter (with a 20d vol-regime scaler). */
    public MarketRegimeDTO marketRegime() {
        LocalDate today = LocalDate.now();
        BarSeries s = indexSeries();
        MarketRegimeDTO.MarketRegimeDTOBuilder b = MarketRegimeDTO.builder()
                .date(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .index(indexTicker);
        if (s == null || s.getBarCount() <= Math.max(tsmomLookback, smaLookback) + 1) {
            return b.regime("No index data").exposure(0).build();
        }
        ClosePriceIndicator close = new ClosePriceIndicator(s);
        int last = s.getEndIndex();

        double tsmom = new ROCIndicator(close, tsmomLookback).getValue(last).doubleValue();
        boolean tsmomBull = tsmom > 0;

        double sma = new SMAIndicator(close, smaLookback).getValue(last).doubleValue();
        double closePx = close.getValue(last).doubleValue();
        double closeVsSmaPct = sma > 0 ? (closePx - sma) / sma * 100.0 : 0.0;
        // ±buffer around the SMA to damp whipsaws: bull only when clearly above.
        boolean sma200Bull = closeVsSmaPct > -smaBufferPct;

        double volRatio = indexVolRatio(close, last);
        boolean volElevated = volRatio > volElevatedMult;

        double exposure = (tsmomBull && sma200Bull) ? 1.0 : (tsmomBull || sma200Bull) ? 0.5 : 0.0;
        if (volElevated) {
            exposure *= 0.5;
        }
        String regime = exposure >= 1.0 ? "Bull — full exposure"
                : exposure > 0 ? "Mixed — reduced exposure" : "Bear — defensive / cash";

        return b.tsmom252Pct(round(tsmom)).tsmomBull(tsmomBull)
                .closeVsSma200Pct(round(closeVsSmaPct)).sma200Bull(sma200Bull)
                .volRatio(round(volRatio)).volElevated(volElevated)
                .exposure(exposure).regime(regime)
                .build();
    }

    /** S5 — turn-of-month timing (last 2 + first 3 trading days). */
    public TurnOfMonthDTO turnOfMonth() {
        LocalDate today = LocalDate.now();
        boolean inWindow = TradingCalendar.isTurnOfMonthWindow(today);
        return TurnOfMonthDTO.builder()
                .date(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .inWindow(inWindow)
                .guidance(inWindow ? "In the turn-of-month window — favourable for small/mid-cap entries."
                        : "Outside the turn-of-month window.")
                .build();
    }

    /** S8 — Nordic calendar anomalies (payday-shifted ToM + reporting-month pre-drift). */
    public NordicCalendarDTO nordicCalendar() {
        LocalDate today = LocalDate.now();
        boolean payday = TradingCalendar.isNordicPaydayTomWindow(today);
        boolean report = TradingCalendar.isReportMonth(today);
        String guidance = payday
                ? "Payday-shifted turn-of-month window (~27th–4th) — Swedish fund inflows."
                : report ? "Reporting month — pre-report drift tends positive; avoid holding through the report."
                         : "No Nordic calendar signal active.";
        return NordicCalendarDTO.builder()
                .date(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .paydayTomWindow(payday)
                .reportMonth(report)
                .guidance(guidance)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private double indexMomentum() {
        BarSeries s = indexSeries();
        if (s == null || s.getBarCount() <= tsmomLookback + 1) {
            return 0.0;
        }
        return new ROCIndicator(new ClosePriceIndicator(s), tsmomLookback).getValue(s.getEndIndex()).doubleValue();
    }

    /** Current 20d stdev of close vs the median of that stdev over the last {@code volMedianBars}. */
    private double indexVolRatio(ClosePriceIndicator close, int last) {
        StandardDeviationIndicator sd = new StandardDeviationIndicator(close, volLookback);
        double current = sd.getValue(last).doubleValue();
        int from = Math.max(close.getBarSeries().getBeginIndex() + volLookback, last - volMedianBars + 1);
        double[] vals = new double[last - from + 1];
        for (int i = from; i <= last; i++) {
            vals[i - from] = sd.getValue(i).doubleValue();
        }
        double median = median(vals);
        return median > 0 ? current / median : 1.0;
    }

    static double median(double[] x) {
        if (x.length == 0) return 0;
        double[] c = x.clone();
        Arrays.sort(c);
        int n = c.length;
        return (n % 2 == 1) ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2.0;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
