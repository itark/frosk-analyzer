package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Calculates the Opening Range width as a percentage of the OR low.
 *
 * <p>Width = (OR_high - OR_low) / OR_low * 100
 *
 * <p>Used to filter out days where the Opening Range is too narrow (no volatility,
 * breakout won't have momentum) or too wide (excessive risk per trade).
 */
public class OpeningRangeWidthIndicator extends CachedIndicator<Num> {

    private final OpeningRangeHighIndicator orHigh;
    private final OpeningRangeLowIndicator orLow;

    public OpeningRangeWidthIndicator(BarSeries series, int orBars) {
        super(series);
        this.orHigh = new OpeningRangeHighIndicator(series, orBars);
        this.orLow = new OpeningRangeLowIndicator(series, orBars);
    }

    @Override
    protected Num calculate(int index) {
        Num high = orHigh.getValue(index);
        Num low = orLow.getValue(index);
        if (low.isZero()) {
            return numOf(0);
        }
        return high.minus(low).dividedBy(low).multipliedBy(numOf(100));
    }

    @Override
    public int getUnstableBars() {
        return orHigh.getUnstableBars();
    }
}
