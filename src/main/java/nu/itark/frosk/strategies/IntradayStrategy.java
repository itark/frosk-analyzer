package nu.itark.frosk.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

/**
 * Marker interface for strategies that participate in the Tier-0 intraday pipeline.
 *
 * <p>Implement this on any {@link AbstractStrategy} subclass that should be
 * auto-discovered and evaluated by {@link nu.itark.frosk.service.IntradayStrategyRunner}.
 */
public interface IntradayStrategy {
    Strategy buildStrategy(BarSeries series);
}
