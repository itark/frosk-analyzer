package nu.itark.frosk.strategies;

import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;

/**
 * Intraday (15-minute bar) variant of {@link RunawayGAPStrategy}.
 *
 * <p>Same entry/exit logic — separate class name so that {@code FeaturedStrategy}
 * rows for the intraday timeframe don't collide with the daily-bar version.
 */
@Component
public class RunawayGAPIntradayStrategy extends RunawayGAPStrategy {

    @Override
    public Strategy buildStrategy(BarSeries series) {
        Strategy base = super.buildStrategy(series);
        return new BaseStrategy(this.getClass().getSimpleName(),
                base.getEntryRule(), base.getExitRule());
    }
}
