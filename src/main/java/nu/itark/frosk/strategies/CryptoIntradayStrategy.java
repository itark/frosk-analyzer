package nu.itark.frosk.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

/**
 * Marker interface for strategies that participate in the CRYPTO intraday
 * pipeline (15m Coinbase bars, 24/7).
 *
 * <p>Deliberately separate from {@link IntradayStrategy}: that marker is
 * auto-discovered by the equity Tier-0 runner, and crypto strategies must not
 * run on OMX30 series (and vice versa). Implementations are auto-discovered
 * by {@link nu.itark.frosk.service.CryptoIntradayStrategyRunner}.
 */
public interface CryptoIntradayStrategy {
    Strategy buildStrategy(BarSeries series);

    /** True for short-side strategies — runner emits "SHRT"/"COVR" instead of "BUY"/"SELL". */
    default boolean isShort() { return false; }
}
