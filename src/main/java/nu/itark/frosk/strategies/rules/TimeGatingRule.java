package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Blocks entry during a fixed UTC time window.
 * Returns {@code false} (prevents entry) when bar {@code index}'s end time
 * falls within [{@code blockStart}, {@code blockEnd)} UTC.
 * Handles overnight windows (blockStart > blockEnd) correctly.
 *
 * <p>Evaluated against the bar's own end time, not wall-clock "now" — this
 * rule is walked by {@code BarSeriesManager} over the strategy's entire
 * history (live evaluation only ever looks at the last bar, but
 * {@link nu.itark.frosk.analysis.StrategyExecutor}'s {@code FeaturedStrategy}
 * backtest replays every bar). Gating on wall-clock time made every bar in
 * that replay share whatever time of day the backtest happened to run at —
 * the whole history was either always blocked or never blocked, at random,
 * silently corrupting the backtest trade count and SQN for
 * {@code CryptoShortIntradayStrategy} and {@code CryptoEMACrossShortIntradayStrategy}
 * (the two strategies that use this rule). Confirmed harmless for live
 * evaluation, where the last bar's end time is already ≈ now.
 */
public class TimeGatingRule extends AbstractRule {

    private final BarSeries barSeries;
    private final LocalTime blockStart;
    private final LocalTime blockEnd;

    public TimeGatingRule(BarSeries barSeries, LocalTime blockStart, LocalTime blockEnd) {
        this.barSeries = barSeries;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        LocalTime barTime = barSeries.getBar(index).getEndTime()
                .withZoneSameInstant(ZoneOffset.UTC).toLocalTime();
        if (blockStart.isBefore(blockEnd)) {
            // Normal window [blockStart, blockEnd): allow when outside the window
            return barTime.isBefore(blockStart) || !barTime.isBefore(blockEnd);
        }
        // Overnight window (e.g. 23:00–02:00): allow when now is in [blockEnd, blockStart)
        return !barTime.isBefore(blockEnd) && barTime.isBefore(blockStart);
    }
}
