package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.forecast.LstmBar;
import nu.itark.frosk.forecast.LstmServiceClient;
import nu.itark.frosk.forecast.LstmSignalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls the LSTM signal-filter microservice with the trailing {@value
 * #BARS_COUNT} bars for a ticker and reduces its response to a single
 * boolean gate: is this a confident BUY signal.
 *
 * <p>Same shape as {@link RegimeForecastService}: an {@code enabled} flag
 * gates whether any HTTP call is made at all (callers must check {@link
 * #isEnabled()} before wiring a gate — see
 * {@link nu.itark.frosk.strategies.CryptoEMACrossLongIntradayStrategy} for
 * the call-site pattern), a per-(ticker, bar-index) cache avoids redundant
 * calls when multiple rules ask about the same bar, and the client never
 * throws — a failed/unreachable service, too little history, or a {@code
 * fallback=true} response (the service's own degraded-default signal — not
 * treated as a genuine prediction) all fail closed to "not a buy signal".
 */
@Service
@Slf4j
public class LstmSignalFilterService {

    /** Trailing bars sent per request — the model's fixed input window. */
    private static final int BARS_COUNT = 25;

    private final LstmServiceClient lstmServiceClient;
    private final boolean enabled;
    private final double threshold;

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public LstmSignalFilterService(LstmServiceClient lstmServiceClient,
                                    @Value("${forecast.lstm.enabled:false}") boolean enabled,
                                    @Value("${forecast.lstm.signal.threshold:0.55}") double threshold) {
        this.lstmServiceClient = lstmServiceClient;
        this.enabled = enabled;
        this.threshold = threshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * True when the LSTM service returns a genuine (non-fallback) "BUY"
     * prediction with {@code signal_probability >= threshold} for the {@value
     * #BARS_COUNT} bars ending at {@code index}. Cached per (series name,
     * index). Returns {@code false} immediately, with no HTTP call, when
     * disabled or when fewer than {@value #BARS_COUNT} bars are available.
     */
    public boolean isBuySignal(BarSeries series, int index) {
        if (!enabled) {
            return false;
        }
        String cacheKey = series.getName() + "|" + index;
        Boolean cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean result = computeBuySignal(series, index);
        cache.put(cacheKey, result);
        return result;
    }

    /** Clears the cache — call if a security's bar history is ever rebuilt from scratch. */
    public void clearCache() {
        cache.clear();
    }

    private boolean computeBuySignal(BarSeries series, int index) {
        List<LstmBar> bars = trailingBars(series, index);
        if (bars.size() < BARS_COUNT) {
            log.debug("LstmSignalFilterService: {} has only {} bars at index {} (need {}) — not a buy signal",
                    series.getName(), bars.size(), index, BARS_COUNT);
            return false;
        }

        Optional<LstmSignalResponse> response = lstmServiceClient.fetchSignal(series.getName(), bars);
        if (response.isEmpty()) {
            log.warn("LstmSignalFilterService: LSTM service unreachable/failed for {} at index {} — not a buy signal (fail closed)",
                    series.getName(), index);
            return false;
        }

        LstmSignalResponse signal = response.get();
        if (signal.fallback()) {
            log.info("LstmSignalFilterService: {} LSTM service returned fallback=true — not a buy signal", series.getName());
            return false;
        }

        boolean isBuy = "BUY".equals(signal.prediction()) && signal.signalProbability() >= threshold;
        log.info("LstmSignalFilterService: {} prediction={} probability={} threshold={} -> buySignal={}",
                series.getName(), signal.prediction(), signal.signalProbability(), threshold, isBuy);
        return isBuy;
    }

    /** Trailing {@value #BARS_COUNT} bars ending at {@code index}, oldest first. */
    private List<LstmBar> trailingBars(BarSeries series, int index) {
        int from = Math.max(0, index - BARS_COUNT + 1);
        List<LstmBar> bars = new ArrayList<>();
        for (int i = from; i <= index; i++) {
            Bar bar = series.getBar(i);
            bars.add(new LstmBar(
                    bar.getOpenPrice().doubleValue(),
                    bar.getHighPrice().doubleValue(),
                    bar.getLowPrice().doubleValue(),
                    bar.getClosePrice().doubleValue(),
                    bar.getVolume().doubleValue()));
        }
        return bars;
    }
}
