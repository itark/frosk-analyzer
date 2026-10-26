package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

/**
 * Z-score of the log-price spread between a primary series (this indicator's
 * {@link BarSeries}) and a secondary series, with a rolling OLS hedge ratio.
 *
 * <p>The signal engine for the BTC/ETH stat-arb recipe (Strategi 4). At each
 * bar it refits β over the trailing {@code window} of
 * {@code (log secondary, log primary)} pairs, forms the spread
 * {@code s = log(primary) − β·log(secondary)} across the window, and reports
 * the current spread's z-score against that window's mean and standard
 * deviation. A low z means the primary is cheap relative to the secondary —
 * the entry side for a long-the-spread (long primary) position.
 *
 * <p>The secondary closes arrive pre-aligned to the primary bar index as a
 * {@code double[]} (built by the strategy from the secondary's own series,
 * floor-matched on bar end time), with {@code NaN} where no secondary bar
 * exists at or before a primary bar. Any {@code NaN} in the window makes the
 * bar's z {@code NaN}, so entry rules simply don't fire there.
 */
public class PairSpreadZScoreIndicator extends CachedIndicator<Num> {

    private final ClosePriceIndicator primaryClose;
    private final double[] secondaryClose;
    private final int window;

    public PairSpreadZScoreIndicator(BarSeries primarySeries, double[] secondaryClose, int window) {
        super(primarySeries);
        if (window < 2) {
            throw new IllegalArgumentException("window must be >= 2, was " + window);
        }
        this.primaryClose = new ClosePriceIndicator(primarySeries);
        this.secondaryClose = secondaryClose;
        this.window = window;
    }

    @Override
    protected Num calculate(int index) {
        int from = index - window + 1;
        if (from < 0) {
            return NaN;
        }

        double[] logX = new double[index + 1]; // secondary
        double[] logY = new double[index + 1]; // primary
        for (int i = from; i <= index; i++) {
            double p = primaryClose.getValue(i).doubleValue();
            double s = secondaryClose[i];
            if (Double.isNaN(s) || s <= 0 || p <= 0) {
                return NaN;
            }
            logX[i] = Math.log(s);
            logY[i] = Math.log(p);
        }

        OLSRegression ols;
        try {
            ols = OLSRegression.fit(logX, logY, from, index);
        } catch (IllegalArgumentException e) {
            return NaN;
        }
        double beta = ols.beta();

        double sum = 0;
        double sumSq = 0;
        for (int i = from; i <= index; i++) {
            double spread = logY[i] - beta * logX[i];
            sum += spread;
            sumSq += spread * spread;
        }
        double mean = sum / window;
        double variance = sumSq / window - mean * mean;
        if (variance <= 0) {
            return NaN;
        }
        double std = Math.sqrt(variance);

        double currentSpread = logY[index] - beta * logX[index];
        return numOf((currentSpread - mean) / std);
    }

    @Override
    public int getUnstableBars() {
        return window;
    }
}
