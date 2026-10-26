package nu.itark.frosk.strategies.indicators;

/**
 * Ordinary least-squares fit of {@code y = alpha + beta·x} over paired samples.
 *
 * <p>Deliberately dependency-free (no Apache Commons Math) — the stat-arb
 * recipe (Strategi 4) needs only the slope (the hedge ratio β) and, for the
 * spread, the intercept. A SSRN comparison cited in the research found a static
 * OLS hedge ratio beat a Kalman filter net of costs (far lower turnover), so a
 * plain rolling OLS is the deliberate choice, not a placeholder.
 */
public final class OLSRegression {

    private final double beta;
    private final double alpha;

    private OLSRegression(double beta, double alpha) {
        this.beta = beta;
        this.alpha = alpha;
    }

    /** Slope — the hedge ratio β of y on x. */
    public double beta() {
        return beta;
    }

    /** Intercept α. */
    public double alpha() {
        return alpha;
    }

    /**
     * Fits over {@code x[from..to]} / {@code y[from..to]} inclusive.
     *
     * @throws IllegalArgumentException if fewer than two points, or x has zero variance
     */
    public static OLSRegression fit(double[] x, double[] y, int from, int to) {
        int n = to - from + 1;
        if (n < 2) {
            throw new IllegalArgumentException("Need at least two points, got " + n);
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = from; i <= to; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }
        double denom = n * sumXX - sumX * sumX;
        if (denom == 0.0) {
            throw new IllegalArgumentException("x has zero variance over the window");
        }
        double beta = (n * sumXY - sumX * sumY) / denom;
        double alpha = (sumY - beta * sumX) / n;
        return new OLSRegression(beta, alpha);
    }
}
