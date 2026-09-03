package nu.itark.frosk.service;

import nu.itark.frosk.model.Security;
import org.ta4j.core.BarSeries;

import java.util.Map;

/**
 * Strategy-neutral contract for the 15-minute intraday data pipeline.
 *
 * <p>Two implementations exist, one per broker profile:
 * <ul>
 *   <li>{@link CryptoIntradayDataService} — {@code crypto} profile, pulls candles from Coinbase.</li>
 *   <li>{@link KrakenFuturesIntradayDataService} — {@code kraken-futures} profile, pulls candles
 *       from Kraken Futures history API.</li>
 * </ul>
 *
 * Consumers (e.g. {@link CryptoIntradayStrategyRunner}) inject this interface so that the
 * concrete data source is transparent to the strategy layer.
 */
public interface IntradayDataService {

    /**
     * Sync fresh candles from the upstream API, persist them, and return a
     * {@link BarSeries} per active security. May prune bars outside the retention window.
     */
    Map<Security, BarSeries> syncAndBuildAllSeries();

    /**
     * Build {@link BarSeries} from already-persisted bars only — no network I/O.
     * Used by offline backtest and reporting tooling.
     */
    Map<Security, BarSeries> buildAllSeriesFromDb();
}
