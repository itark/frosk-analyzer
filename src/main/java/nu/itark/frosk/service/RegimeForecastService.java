package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.regime.GarchServiceClient;
import nu.itark.frosk.regime.Regime;
import nu.itark.frosk.regime.VolatilityRegimeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combines the Python GARCH(1,1) volatility-regime microservice with a local
 * ta4j ADX trend-strength check into a single {@link Regime}.
 *
 * <p>GARCH describes volatility, not direction — it cannot by itself
 * distinguish TRENDING from SIDEWAYS. This service owns that composition:
 * the Python side classifies LOW/NORMAL/HIGH volatility only; ADX({@value
 * #ADX_PERIOD}), computed here from the same {@link BarSeries} the caller
 * already holds, decides TRENDING vs SIDEWAYS whenever volatility is not
 * HIGH. HIGH volatility always maps to {@link Regime#VOLATILE}, regardless
 * of ADX.
 *
 * <p><b>Fail-closed contract:</b> {@link Regime#UNKNOWN} is returned for too
 * little history, a GARCH fit that did not converge, or any client-side
 * failure (timeout, connection refused, malformed response). UNKNOWN never
 * equals a strategy's required regime, so a gate built on this service
 * blocks entries rather than letting them through on missing data.
 *
 * <p><b>Disabled contract:</b> when {@code regime.garch.enabled=false}
 * (the default in every profile except {@code equity}), {@link #getRegime}
 * still returns {@link Regime#UNKNOWN} — but the caller must check
 * {@link #isEnabled()} BEFORE deciding whether to wire a gate at all, and
 * substitute a no-op {@code BooleanRule(true)} when disabled. That mirrors
 * {@code AbstractStrategy.liquidityRule()}'s own feature-flag pattern: a
 * disabled feature must never silently suppress every signal. See
 * {@link nu.itark.frosk.strategies.ShortTermMomentumLongTermStrengthStrategy}
 * for the call-site pattern. When disabled, {@link #getRegime} never invokes
 * the HTTP client — no network traffic is possible.
 */
@Service
@Slf4j
public class RegimeForecastService {

    private static final int ADX_PERIOD = 14;

    private final GarchServiceClient garchServiceClient;
    private final boolean enabled;
    private final int minObservations;
    private final int windowBars;
    private final double adxTrendThreshold;

    private final Map<String, Regime> cache = new ConcurrentHashMap<>();

    public RegimeForecastService(GarchServiceClient garchServiceClient,
                                  @Value("${regime.garch.enabled:false}") boolean enabled,
                                  @Value("${regime.garch.min.observations:200}") int minObservations,
                                  @Value("${regime.garch.window.bars:1000}") int windowBars,
                                  @Value("${regime.garch.trend.adx.threshold:25.0}") double adxTrendThreshold) {
        this.garchServiceClient = garchServiceClient;
        this.enabled = enabled;
        this.minObservations = minObservations;
        this.windowBars = windowBars;
        this.adxTrendThreshold = adxTrendThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Regime at bar {@code index} of {@code series}. Cached per (series name,
     * index) for the lifetime of this bean. Returns {@link Regime#UNKNOWN}
     * immediately, with no HTTP call, when disabled or when fewer than
     * {@code regime.garch.min.observations} returns are available.
     */
    public Regime getRegime(BarSeries series, int index) {
        if (!enabled) {
            return Regime.UNKNOWN;
        }
        String cacheKey = series.getName() + "|" + index;
        Regime cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Regime regime = computeRegime(series, index);
        cache.put(cacheKey, regime);
        return regime;
    }

    /** Clears the cache — call if a security's bar history is ever rebuilt from scratch. */
    public void clearCache() {
        cache.clear();
    }

    private Regime computeRegime(BarSeries series, int index) {
        List<Double> returns = logReturns(series, index);
        if (returns.size() < minObservations) {
            log.debug("RegimeForecastService: {} has only {} returns at index {} (need {}) — UNKNOWN",
                    series.getName(), returns.size(), index, minObservations);
            return Regime.UNKNOWN;
        }

        Optional<VolatilityRegimeResponse> response = garchServiceClient.fetchVolatilityRegime(series.getName(), returns);
        if (response.isEmpty()) {
            log.warn("RegimeForecastService: GARCH service unreachable/failed for {} at index {} — UNKNOWN (fail closed)",
                    series.getName(), index);
            return Regime.UNKNOWN;
        }

        VolatilityRegimeResponse vol = response.get();
        if (!vol.converged() || "UNKNOWN".equals(vol.volatilityRegime())) {
            log.info("RegimeForecastService: {} GARCH fit did not converge (n={}) — UNKNOWN",
                    series.getName(), vol.nObservations());
            return Regime.UNKNOWN;
        }
        if ("HIGH".equals(vol.volatilityRegime())) {
            log.info("RegimeForecastService: {} GARCH volatility=HIGH (percentile={}, condVol={}) -> Regime.VOLATILE",
                    series.getName(), vol.volatilityPercentile(), vol.conditionalVolatility());
            return Regime.VOLATILE;
        }

        double adxValue = new ADXIndicator(series, ADX_PERIOD).getValue(index).doubleValue();
        Regime regime = adxValue > adxTrendThreshold ? Regime.TRENDING : Regime.SIDEWAYS;
        log.info("RegimeForecastService: {} GARCH volatility={} (percentile={}, condVol={}), ADX={} -> Regime.{}",
                series.getName(), vol.volatilityRegime(), vol.volatilityPercentile(), vol.conditionalVolatility(),
                adxValue, regime);
        return regime;
    }

    /** Trailing log-returns ending at {@code index}, capped to {@code windowBars}. */
    private List<Double> logReturns(BarSeries series, int index) {
        int from = Math.max(1, index - windowBars + 1);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        List<Double> returns = new ArrayList<>();
        for (int i = from; i <= index; i++) {
            double prev = close.getValue(i - 1).doubleValue();
            double curr = close.getValue(i).doubleValue();
            if (prev <= 0 || curr <= 0) {
                continue;
            }
            returns.add(Math.log(curr / prev));
        }
        return returns;
    }
}
