package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Median traded value (volume × close) over the last {@code barCount} bars, in the
 * instrument's own currency.
 *
 * <p>Median rather than mean on purpose: turnover is heavily right-skewed — a single
 * block trade or news day can lift a mean far above what can be traded on a normal
 * day, which is exactly the day the strategy would need to get filled.
 *
 * <p>Returns zero until {@code barCount} bars are available, so a
 * {@link nu.itark.frosk.strategies.rules.LiquidityRule} built on it blocks entries
 * on freshly listed instruments rather than guessing.
 */
public class MedianTurnoverIndicator extends CachedIndicator<Num> {

    private final int barCount;

    public MedianTurnoverIndicator(BarSeries series, int barCount) {
        super(series);
        if (barCount <= 0) {
            throw new IllegalArgumentException("barCount must be positive, was " + barCount);
        }
        this.barCount = barCount;
    }

    @Override
    protected Num calculate(int index) {
        if (index < barCount - 1) {
            return numOf(0);
        }
        List<Num> turnovers = new ArrayList<>(barCount);
        for (int i = index - barCount + 1; i <= index; i++) {
            Num volume = getBarSeries().getBar(i).getVolume();
            Num close = getBarSeries().getBar(i).getClosePrice();
            if (volume == null || close == null) {
                continue;
            }
            turnovers.add(volume.multipliedBy(close));
        }
        if (turnovers.isEmpty()) {
            return numOf(0);
        }
        Collections.sort(turnovers);
        int mid = turnovers.size() / 2;
        if (turnovers.size() % 2 == 1) {
            return turnovers.get(mid);
        }
        return turnovers.get(mid - 1).plus(turnovers.get(mid)).dividedBy(numOf(2));
    }

    @Override
    public int getUnstableBars() {
        return barCount;
    }
}
