package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.BtcTrendChartPoint;
import nu.itark.frosk.analysis.BtcTrendSignal;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.strategies.CryptoBTCTrendStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily BTC-EUR trend-follower signal — the thing a human acts on manually via
 * Avanza (BULL BITCOIN X2 AVA on LONG, BEAR BITCOIN X2 AVA on SHORT, hold cash
 * on NEUTRAL).
 *
 * <h3>Data source</h3>
 * The daily BTC-EUR OHLCV series in {@code security_price}, synced nightly from
 * the Coinbase Advanced Trade candles API by {@code COINBASEDataManager}
 * ({@code Scheduler.cryptoDailySync()} at 00:30). No Yahoo Finance — the crypto
 * process has never needed it; this is the same table {@link CryptoRegimeService}
 * reads BTC daily closes from.
 *
 * <h3>Signal</h3>
 * EMA({@code emaFast})/EMA({@code emaSlow}) crossover with an ADX({@code adxPeriod})
 * &gt; {@code adxThreshold} filter, evaluated at the last daily bar:
 * <ul>
 *   <li>ADX &lt; threshold → NEUTRAL (no clear trend, hold cash)</li>
 *   <li>EMA fast &gt; EMA slow → LONG</li>
 *   <li>EMA fast &lt; EMA slow → SHORT</li>
 * </ul>
 * The ta4j expression of the long leg lives in {@link CryptoBTCTrendStrategy}
 * (registered in {@code StrategiesMap}); this service reuses that class's
 * configured parameters and produces the three-state signal the ta4j
 * long-only model cannot.
 *
 * <h3>Scheduling</h3>
 * {@code @Scheduled} daily just after the 00:30 price sync. Each run refreshes
 * the {@code featured_strategy} row named "BTCTrendFollower" via the normal
 * {@link StrategyExecutor} path (backtest metrics on BTC-EUR's history) and
 * logs the current signal. The {@code GET /crypto/btc-trend-signal} endpoint
 * ({@code CryptoDashboardController}) recomputes the signal fresh on every call
 * for the dashboard card.
 */
@Service
@Profile("crypto")
@Slf4j
public class CryptoBtcTrendSignalService {

    /** featured_strategy.name — what the dashboard strategy list shows. */
    public static final String FEATURED_NAME = "BTCTrendFollower";

    /** {@code GET /crypto/btc-trend-chart?days=} bounds. */
    public static final int DEFAULT_CHART_DAYS = 90;
    public static final int MIN_CHART_DAYS = 7;
    public static final int MAX_CHART_DAYS = 365;

    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Full EMA/ADX time series (oldest→newest, warmup bars dropped), rebuilt at
     * most once per UTC day — recomputing every indicator over ~400 bars on
     * every dashboard poll is wasteful. Busted by {@link #clearChartCache()}
     * after the nightly price sync so a fresh bar shows up immediately.
     */
    private volatile List<BtcTrendChartPoint> chartCache = null;
    private volatile LocalDate chartCacheDate = null;

    @Value("${crypto.btctrend.product:BTC-EUR}")
    private String product;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private BarSeriesService barSeriesService;

    @Autowired
    private StrategyExecutor strategyExecutor;

    @Autowired
    private CryptoBTCTrendStrategy strategy;

    // ── public API ───────────────────────────────────────────────────────

    /** Current signal, recomputed from the persisted daily BTC-EUR series. Never throws. */
    public BtcTrendSignal computeSignal() {
        double adxThreshold = strategy.getAdxThreshold();
        try {
            Security btc = securityRepository.findByName(product);
            if (btc == null) {
                log.warn("CryptoBtcTrendSignalService: security '{}' not found — signal unavailable", product);
                return BtcTrendSignal.unavailable(adxThreshold, null);
            }

            BarSeries series = barSeriesService.getDataSet(btc.getId());
            int emaFast = strategy.getEmaFast();
            int emaSlow = strategy.getEmaSlow();
            int adxPeriod = strategy.getAdxPeriod();
            int warmup = emaSlow + 2 * adxPeriod;

            if (series == null || series.getBarCount() <= warmup) {
                int have = series == null ? 0 : series.getBarCount();
                log.warn("CryptoBtcTrendSignalService: only {} daily {} bars (need > {}) — signal unavailable",
                        have, product, warmup);
                java.time.LocalDate asOf = (series != null && series.getBarCount() > 0)
                        ? series.getLastBar().getEndTime().withZoneSameInstant(UTC).toLocalDate() : null;
                return BtcTrendSignal.unavailable(adxThreshold, asOf);
            }

            ClosePriceIndicator close = new ClosePriceIndicator(series);
            EMAIndicator emaFastInd = new EMAIndicator(close, emaFast);
            EMAIndicator emaSlowInd = new EMAIndicator(close, emaSlow);
            ADXIndicator adxInd = new ADXIndicator(series, adxPeriod);

            int last = series.getEndIndex();
            String current = signalAt(last, emaFastInd, emaSlowInd, adxInd, adxThreshold);

            // Walk back to the first bar (after warmup) where the signal was already `current`.
            int changeIdx = last;
            for (int i = last - 1; i >= warmup; i--) {
                if (!signalAt(i, emaFastInd, emaSlowInd, adxInd, adxThreshold).equals(current)) {
                    break;
                }
                changeIdx = i;
            }
            int daysSinceChange = last - changeIdx;
            java.time.LocalDate lastChangeDate =
                    series.getBar(changeIdx).getEndTime().withZoneSameInstant(UTC).toLocalDate();
            java.time.LocalDate asOf =
                    series.getBar(last).getEndTime().withZoneSameInstant(UTC).toLocalDate();

            return new BtcTrendSignal(
                    current,
                    BtcTrendSignal.avanzaInstrumentFor(current),
                    scaled(emaFastInd.getValue(last).doubleValue()),
                    scaled(emaSlowInd.getValue(last).doubleValue()),
                    scaled(adxInd.getValue(last).doubleValue()),
                    adxThreshold,
                    daysSinceChange,
                    lastChangeDate,
                    asOf,
                    true);
        } catch (Exception e) {
            log.warn("CryptoBtcTrendSignalService: signal computation failed — {}", e.toString());
            return BtcTrendSignal.unavailable(adxThreshold, null);
        }
    }

    /**
     * Daily close / EMA{fast} / EMA{slow} / ADX{period} series for the dashboard
     * chart, newest last. {@code days} is clamped to
     * [{@value #MIN_CHART_DAYS}, {@value #MAX_CHART_DAYS}]; the warmup bars the
     * indicators need are never returned. Empty list when there is not enough
     * BTC-EUR history yet. Never throws.
     */
    public List<BtcTrendChartPoint> computeChart(int days) {
        int clamped = Math.max(MIN_CHART_DAYS, Math.min(MAX_CHART_DAYS, days));
        List<BtcTrendChartPoint> full = fullChartSeries();
        if (full.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, full.size() - clamped);
        return List.copyOf(full.subList(from, full.size()));
    }

    // ── scheduled refresh ────────────────────────────────────────────────

    /** Populate the featured_strategy row once on startup so the dashboard isn't empty after a restart. */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("CryptoBtcTrendSignalService: startup refresh failed — {}", e.toString());
        }
    }

    /**
     * Runs just after the 00:30 Coinbase price sync. Refreshes the
     * "BTCTrendFollower" featured_strategy row via the standard executor path
     * and logs the current signal.
     */
    @Scheduled(cron = "${scheduler.crypto.btctrend.cron:0 35 0 * * *}")
    public void refresh() {
        Security btc = securityRepository.findByName(product);
        if (btc == null) {
            log.warn("CryptoBtcTrendSignalService: security '{}' not found — skipping refresh", product);
            return;
        }
        BarSeries series = barSeriesService.getDataSet(btc.getId());
        if (series == null || series.isEmpty()) {
            log.warn("CryptoBtcTrendSignalService: no daily {} bars — skipping refresh", product);
            return;
        }

        try {
            strategyExecutor.execute(FEATURED_NAME, List.of(series));
        } catch (Exception e) {
            log.warn("CryptoBtcTrendSignalService: featured_strategy refresh failed — {}", e.toString());
        }

        clearChartCache();

        BtcTrendSignal s = computeSignal();
        log.info("CryptoBtcTrendSignalService: {} signal = {} ({}) — EMA{}={} EMA{}={} ADX{}={} — "
                        + "{} days since last change ({})",
                product, s.signal(), s.avanzaInstrument(),
                strategy.getEmaFast(), s.ema10(), strategy.getEmaSlow(), s.ema20(),
                strategy.getAdxPeriod(), s.adx(), s.daysSinceSignalChange(), s.lastSignalChangeDate());
    }

    // ── private ──────────────────────────────────────────────────────────

    static String signalAt(int index, EMAIndicator fast, EMAIndicator slow,
                           ADXIndicator adx, double adxThreshold) {
        return classify(fast.getValue(index).doubleValue(), slow.getValue(index).doubleValue(),
                adx.getValue(index).doubleValue(), adxThreshold);
    }

    /**
     * The three-state trend rule, isolated so it is trivially unit-testable:
     * <ul>
     *   <li>{@code adx < adxThreshold} → NEUTRAL (no tradeable trend)</li>
     *   <li>{@code emaFast > emaSlow}  → LONG</li>
     *   <li>otherwise                  → SHORT</li>
     * </ul>
     * ADX exactly at the threshold counts as trending (the check is strict
     * {@code <}); equal EMAs resolve to SHORT.
     */
    static String classify(double emaFast, double emaSlow, double adx, double adxThreshold) {
        if (adx < adxThreshold) {
            return BtcTrendSignal.NEUTRAL;
        }
        return emaFast > emaSlow ? BtcTrendSignal.LONG : BtcTrendSignal.SHORT;
    }

    private static BigDecimal scaled(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /** Drops the cached chart series so the next request rebuilds it. */
    private synchronized void clearChartCache() {
        chartCache = null;
        chartCacheDate = null;
    }

    /** Cached full chart series, rebuilt at most once per UTC day. */
    private List<BtcTrendChartPoint> fullChartSeries() {
        LocalDate today = LocalDate.now(UTC);
        List<BtcTrendChartPoint> cached = chartCache;
        if (cached != null && today.equals(chartCacheDate)) {
            return cached;
        }
        synchronized (this) {
            if (chartCache != null && today.equals(chartCacheDate)) {
                return chartCache;
            }
            List<BtcTrendChartPoint> built = buildFullChartSeries();
            chartCache = built;
            chartCacheDate = today;
            return built;
        }
    }

    private List<BtcTrendChartPoint> buildFullChartSeries() {
        try {
            Security btc = securityRepository.findByName(product);
            if (btc == null) {
                log.warn("CryptoBtcTrendSignalService: security '{}' not found — chart unavailable", product);
                return List.of();
            }
            BarSeries series = barSeriesService.getDataSet(btc.getId());
            int emaFast = strategy.getEmaFast();
            int emaSlow = strategy.getEmaSlow();
            int adxPeriod = strategy.getAdxPeriod();
            int warmup = emaSlow + 2 * adxPeriod;

            if (series == null || series.getBarCount() <= warmup) {
                int have = series == null ? 0 : series.getBarCount();
                log.warn("CryptoBtcTrendSignalService: only {} daily {} bars (need > {}) — chart unavailable",
                        have, product, warmup);
                return List.of();
            }

            ClosePriceIndicator close = new ClosePriceIndicator(series);
            EMAIndicator emaFastInd = new EMAIndicator(close, emaFast);
            EMAIndicator emaSlowInd = new EMAIndicator(close, emaSlow);
            ADXIndicator adxInd = new ADXIndicator(series, adxPeriod);
            double adxThreshold = strategy.getAdxThreshold();

            List<BtcTrendChartPoint> out = new ArrayList<>();
            for (int i = warmup; i <= series.getEndIndex(); i++) {
                out.add(new BtcTrendChartPoint(
                        series.getBar(i).getEndTime().withZoneSameInstant(UTC).toLocalDate(),
                        scaled(close.getValue(i).doubleValue()),
                        scaled(emaFastInd.getValue(i).doubleValue()),
                        scaled(emaSlowInd.getValue(i).doubleValue()),
                        scaled(adxInd.getValue(i).doubleValue()),
                        signalAt(i, emaFastInd, emaSlowInd, adxInd, adxThreshold)));
            }
            return List.copyOf(out);
        } catch (Exception e) {
            log.warn("CryptoBtcTrendSignalService: chart computation failed — {}", e.toString());
            return List.of();
        }
    }
}
