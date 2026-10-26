package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandWidthIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

/**
 * Rolling percentile rank (0–100) of Bollinger Band width against the last
 * {@code lookback} bars, current bar included.
 *
 * <p>The "squeeze" detector for the BB-squeeze breakout recipe (Strategi 3): a
 * squeeze is a reading below the {@code squeezePct}-th percentile — the band is
 * tighter than it has been for most of the recent window, i.e. compressed
 * volatility that often precedes an expansion. Like {@link
 * AtrPercentileIndicator}, a per-series percentile is scale-free, so the same
 * squeeze threshold works across 37 coins with different volatility profiles
 * without hand-tuning an absolute band-width level per coin.
 *
 * <p>Band width = BollingerBandWidth over an SMA({@code maPeriod}) with
 * {@code k} standard deviations — the standard ta4j Bollinger stack.
 */
public class BollingerBandWidthPercentileIndicator extends CachedIndicator<Num> {

    private final BollingerBandWidthIndicator bbw;
    private final int lookback;

    public BollingerBandWidthPercentileIndicator(BarSeries series, int maPeriod, double k, int lookback) {
        super(series);
        if (lookback < 1) {
            throw new IllegalArgumentException("lookback must be >= 1, was " + lookback);
        }
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(close, maPeriod));
        StandardDeviationIndicator sd = new StandardDeviationIndicator(close, maPeriod);
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, sd, numOf(k));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, sd, numOf(k));
        this.bbw = new BollingerBandWidthIndicator(upper, middle, lower);
        this.lookback = lookback;
    }

    @Override
    protected Num calculate(int index) {
        int from = Math.max(0, index - lookback + 1);
        double current = bbw.getValue(index).doubleValue();
        int count = 0;
        int total = 0;
        for (int i = from; i <= index; i++) {
            double v = bbw.getValue(i).doubleValue();
            if (Double.isNaN(v)) {
                continue;
            }
            total++;
            if (v <= current) {
                count++;
            }
        }
        if (total == 0) {
            return numOf(100);
        }
        return numOf(100.0 * count / total);
    }

    @Override
    public int getUnstableBars() {
        return bbw.getUnstableBars() + lookback;
    }
}
