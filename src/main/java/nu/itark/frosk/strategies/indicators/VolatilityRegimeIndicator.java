package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Three-state volatility regime from an ATR percentile, with hysteresis and
 * N-bar confirmation to stop the regime flickering on noise.
 *
 * <p>Returns a numeric regime code so plain ta4j threshold rules can gate on
 * it: {@link #QUIET} = {@value #QUIET}, {@link #NORMAL} = {@value #NORMAL},
 * {@link #STRESSED} = {@value #STRESSED}.
 *
 * <ul>
 *   <li>Enter STRESSED when the ATR percentile ≥ {@code stressedOn} for
 *       {@code confirmBars} consecutive bars; leave it only once it drops below
 *       {@code stressedOn − hysteresis}.</li>
 *   <li>Enter QUIET when the percentile ≤ {@code quietOn} for {@code confirmBars}
 *       consecutive bars; leave it only once it rises above
 *       {@code quietOn + hysteresis}.</li>
 *   <li>Everything in between is NORMAL.</li>
 * </ul>
 *
 * <p>Parameters follow the LuxAlgo "Volatility Regime Switches" defaults cited
 * in the research: 80/20 thresholds, 10-point hysteresis, 2-bar confirmation,
 * 100-bar lookback. The percentile is read ONLY on bars at or before the one
 * being evaluated (the recursion walks backward to the prior regime), so there
 * is no look-ahead — safe for the {@code FeaturedStrategy} historical replay.
 */
public class VolatilityRegimeIndicator extends CachedIndicator<Num> {

    public static final int QUIET = 0;
    public static final int NORMAL = 1;
    public static final int STRESSED = 2;

    private final AtrPercentileIndicator atrPct;
    private final double stressedOn;
    private final double quietOn;
    private final double hysteresis;
    private final int confirmBars;

    public VolatilityRegimeIndicator(BarSeries series, int atrPeriod, int lookback,
                                     double quietOn, double stressedOn,
                                     double hysteresis, int confirmBars) {
        super(series);
        this.atrPct = new AtrPercentileIndicator(series, atrPeriod, lookback);
        this.quietOn = quietOn;
        this.stressedOn = stressedOn;
        this.hysteresis = hysteresis;
        this.confirmBars = Math.max(1, confirmBars);
    }

    @Override
    protected Num calculate(int index) {
        if (index == 0) {
            return numOf(NORMAL);
        }
        int prev = getValue(index - 1).intValue();
        double p = atrPct.getValue(index).doubleValue();

        switch (prev) {
            case STRESSED:
                return numOf(p < stressedOn - hysteresis ? NORMAL : STRESSED);
            case QUIET:
                return numOf(p > quietOn + hysteresis ? NORMAL : QUIET);
            default: // NORMAL
                if (sustained(index, v -> v >= stressedOn)) {
                    return numOf(STRESSED);
                }
                if (sustained(index, v -> v <= quietOn)) {
                    return numOf(QUIET);
                }
                return numOf(NORMAL);
        }
    }

    /** True when the ATR percentile has satisfied {@code pred} on each of the last {@code confirmBars} bars. */
    private boolean sustained(int index, java.util.function.DoublePredicate pred) {
        if (index - confirmBars + 1 < 0) {
            return false;
        }
        for (int i = index - confirmBars + 1; i <= index; i++) {
            if (!pred.test(atrPct.getValue(i).doubleValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getUnstableBars() {
        return atrPct.getUnstableBars();
    }
}
