package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.forecast.DailyPnlPoint;
import nu.itark.frosk.forecast.ProphetForecastResponse;
import nu.itark.frosk.forecast.ProphetServiceClient;
import nu.itark.frosk.repo.DailyPnlRow;
import nu.itark.frosk.repo.StrategyTradeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates a strategy's {@code strategy_trade} history into a daily PnL
 * series and forwards it to the Python frosk-prophet-service for a forecast.
 *
 * <p>Same shape as {@link RegimeForecastService}: an {@code enabled} flag
 * gates whether any HTTP call is made at all, a per-strategy-per-day cache
 * avoids re-fetching and re-forecasting on every request within the same
 * day, and the client never throws — a failed/unreachable service surfaces
 * as {@link Optional#empty()}, not an exception.
 *
 * <p>Read-only / advisory: nothing in frosk-analyzer currently gates trading
 * decisions on this forecast — it exists to answer {@code GET /forecast/daily-pnl}.
 * Unlike {@link RegimeForecastService}, there is therefore no "disabled must
 * fail open" concern here; disabled simply means "no forecast available".
 */
@Service
@Slf4j
public class DailyPnlForecastService {

    private final ProphetServiceClient prophetServiceClient;
    private final StrategyTradeRepository strategyTradeRepository;
    private final boolean enabled;

    private final Map<String, ProphetForecastResponse> cache = new ConcurrentHashMap<>();

    public DailyPnlForecastService(ProphetServiceClient prophetServiceClient,
                                    StrategyTradeRepository strategyTradeRepository,
                                    @Value("${forecast.prophet.enabled:false}") boolean enabled) {
        this.prophetServiceClient = prophetServiceClient;
        this.strategyTradeRepository = strategyTradeRepository;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Forecast for {@code strategyName}, {@code horizonDays} ahead. Cached
     * per (strategy, today) — cheap to call repeatedly the same day. Returns
     * {@link Optional#empty()} immediately, with no HTTP call, when disabled;
     * otherwise empty only if the Python service is unreachable or errors.
     */
    public Optional<ProphetForecastResponse> getForecast(String strategyName, int horizonDays) {
        if (!enabled) {
            return Optional.empty();
        }
        String cacheKey = strategyName + "|" + horizonDays + "|" + LocalDate.now();
        ProphetForecastResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<DailyPnlPoint> dailyPnl = loadDailyPnl(strategyName);
        Optional<ProphetForecastResponse> response = prophetServiceClient.fetchForecast(strategyName, dailyPnl, horizonDays);
        if (response.isEmpty()) {
            log.warn("DailyPnlForecastService: Prophet service unreachable/failed for {} — no forecast", strategyName);
            return Optional.empty();
        }
        cache.put(cacheKey, response.get());
        return response;
    }

    /** Clears the cache — call if strategy_trade history is ever rebuilt from scratch. */
    public void clearCache() {
        cache.clear();
    }

    private List<DailyPnlPoint> loadDailyPnl(String strategyName) {
        List<DailyPnlRow> rows = strategyTradeRepository.findDailyPnlByStrategyName(strategyName);
        return rows.stream()
                .map(row -> new DailyPnlPoint(row.getTradeDate().toString(), row.getDailyPnl().doubleValue()))
                .toList();
    }
}
