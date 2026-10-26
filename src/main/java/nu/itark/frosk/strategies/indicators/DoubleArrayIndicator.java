package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

/**
 * Exposes a precomputed {@code double[]} (indexed by bar) as a ta4j indicator,
 * so externally derived per-bar values — e.g. a cross-series signal aligned
 * onto this series' bar grid — can drive ordinary threshold rules.
 *
 * <p>{@code Double.NaN} entries map to ta4j {@code NaN}, where threshold rules
 * simply don't fire. Reads out of range also return {@code NaN}.
 */
public class DoubleArrayIndicator extends CachedIndicator<Num> {

    private final double[] values;

    public DoubleArrayIndicator(BarSeries series, double[] values) {
        super(series);
        this.values = values;
    }

    @Override
    protected Num calculate(int index) {
        if (index < 0 || index >= values.length || Double.isNaN(values[index])) {
            return NaN;
        }
        return numOf(values[index]);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
