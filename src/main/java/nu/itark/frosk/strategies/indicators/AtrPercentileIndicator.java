package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Rolling percentile rank (0–100) of ATR({@code atrPeriod}) against the last
 * {@code lookback} bars, current bar included.
 *
 * <p>The volatility-regime leg of the research "EMA + ADX + volatility-regime"
 * recipe (Strategi 1/5): instead of an absolute ATR threshold — meaningless
 * across 37 coins with wildly different price and volatility scales — it ranks
 * the current ATR within the instrument's OWN recent history. A rank is
 * inherently scale-free, so no ATR/price normalization (ATRP) is needed for a
 * single-series percentile: {@code 80} means "ATR is higher than ~80% of the
 * last {@code lookback} bars", the same way on BTC as on a micro-cap alt.
 *
 * <p>Percentile = (count of window ATR values ≤ current) / windowSize × 100.
 * Ties count toward the rank, so a flat window reads ~100.
 */
public class AtrPercentileIndicator extends CachedIndicator<Num> {

    private final ATRIndicator atr;
    private final int lookback;

    public AtrPercentileIndicator(BarSeries series, int atrPeriod, int lookback) {
        super(series);
        if (lookback < 1) {
            throw new IllegalArgumentException("lookback must be >= 1, was " + lookback);
        }
        this.atr = new ATRIndicator(series, atrPeriod);
        this.lookback = lookback;
    }

    @Override
    protected Num calculate(int index) {
        int from = Math.max(0, index - lookback + 1);
        double current = atr.getValue(index).doubleValue();
        int count = 0;
        int total = 0;
        for (int i = from; i <= index; i++) {
            double v = atr.getValue(i).doubleValue();
            if (Double.isNaN(v)) {
                continue;
            }
            total++;
            if (v <= current) {
                count++;
            }
        }
        if (total == 0) {
            return numOf(0);
        }
        return numOf(100.0 * count / total);
    }

    @Override
    public int getUnstableBars() {
        return atr.getUnstableBars() + lookback;
    }
}
