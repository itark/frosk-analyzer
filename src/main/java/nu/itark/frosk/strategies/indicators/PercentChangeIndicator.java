package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

/**
 * Signed percentage change of the source indicator over {@code lookback} bars:
 * {@code (value[i] / value[i-lookback] − 1) × 100}.
 *
 * <p>Used to express both "has this moved a lot" (a breakout, via the raw value)
 * and "has this NOT moved yet" (a laggard, via {@code TransformIndicator.abs}
 * of it) with one small, reusable indicator. Returns {@code NaN} before enough
 * history exists or when the base value is zero.
 */
public class PercentChangeIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> source;
    private final int lookback;

    public PercentChangeIndicator(Indicator<Num> source, int lookback) {
        super(source);
        if (lookback < 1) {
            throw new IllegalArgumentException("lookback must be >= 1, was " + lookback);
        }
        this.source = source;
        this.lookback = lookback;
    }

    @Override
    protected Num calculate(int index) {
        if (index < lookback) {
            return NaN;
        }
        Num base = source.getValue(index - lookback);
        if (base.isZero()) {
            return NaN;
        }
        return source.getValue(index).dividedBy(base).minus(numOf(1)).multipliedBy(numOf(100));
    }

    @Override
    public int getUnstableBars() {
        return lookback;
    }
}
