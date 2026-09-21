package nu.itark.frosk.regime;

/**
 * Three-way market regime used to gate strategy entries.
 *
 * <p>Composed by {@link nu.itark.frosk.service.RegimeForecastService} from two
 * independent signals: the Python GARCH(1,1) microservice classifies
 * volatility (LOW/NORMAL/HIGH), and a local ta4j ADX indicator classifies
 * trend strength. GARCH alone cannot distinguish TRENDING from SIDEWAYS —
 * that is not what conditional volatility measures.
 *
 * <p>{@link #UNKNOWN} is the fail-closed value: missing data, an unreachable
 * GARCH service, or a non-converged fit all map here. It never equals any
 * strategy's required regime, so gates built on it fail closed rather than
 * silently letting entries through.
 */
public enum Regime {
    TRENDING,
    SIDEWAYS,
    VOLATILE,
    UNKNOWN
}
