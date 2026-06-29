package nu.itark.frosk.strategies.rules;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Blocks entry during a fixed UTC time window.
 * Returns {@code false} (prevents entry) when the current wall-clock UTC time
 * falls within [{@code blockStart}, {@code blockEnd}).
 * Handles overnight windows (blockStart > blockEnd) correctly.
 */
public class TimeGatingRule extends AbstractRule {

    private final LocalTime blockStart;
    private final LocalTime blockEnd;

    public TimeGatingRule(LocalTime blockStart, LocalTime blockEnd) {
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        if (blockStart.isBefore(blockEnd)) {
            // Normal window [blockStart, blockEnd): allow when outside the window
            return now.isBefore(blockStart) || !now.isBefore(blockEnd);
        }
        // Overnight window (e.g. 23:00–02:00): allow when now is in [blockEnd, blockStart)
        return !now.isBefore(blockEnd) && now.isBefore(blockStart);
    }
}
