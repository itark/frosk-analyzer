package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

/**
 * Relative volume: the bar's volume divided by its rolling {@code lookback}-bar
 * average volume.
 *
 * <p>Scale-free like {@link AtrPercentileIndicator} — a reading of {@code 0.7}
 * means "this bar traded at 70% of its own recent average", the same
 * interpretation on BTC as on a thin alt, so a single threshold works across
 * the universe without per-coin tuning. Used by the night-reversion recipe to
 * find the LOW-volume bars (reading below 1.0) where an outsized price move is
 * more likely a thin-book dislocation that reverts than a real trend.
 *
 * <p>Defaults to a 7-day lookback (672 bars on 15m). Returns 1.0 (neutral) when
 * the average volume is zero, so a volume-less series never blocks or triggers
 * on a divide-by-zero.
 */
public class RelativeVolumeIndicator extends CachedIndicator<Num> {

    private final VolumeIndicator volume;
    private final SMAIndicator avgVolume;

    public RelativeVolumeIndicator(BarSeries series, int lookback) {
        super(series);
        if (lookback < 1) {
            throw new IllegalArgumentException("lookback must be >= 1, was " + lookback);
        }
        this.volume = new VolumeIndicator(series);
        this.avgVolume = new SMAIndicator(volume, lookback);
    }

    @Override
    protected Num calculate(int index) {
        Num avg = avgVolume.getValue(index);
        if (avg.isZero()) {
            return numOf(1);
        }
        return volume.getValue(index).dividedBy(avg);
    }

    @Override
    public int getUnstableBars() {
        return avgVolume.getUnstableBars();
    }
}
