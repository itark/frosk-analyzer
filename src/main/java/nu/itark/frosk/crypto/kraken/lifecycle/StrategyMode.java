package nu.itark.frosk.crypto.kraken.lifecycle;

/**
 * Where a Kraken Futures strategy's NEW positions are opened.
 *
 * <p>Mode governs entries only. Exits are always routed to every venue — each
 * venue closes a matching position if it has one and is a no-op otherwise — so
 * a position opened under one mode is still closed after the mode changes
 * (e.g. a live position opened in LIVE is closed by its exit signal after a
 * rollback to PAPER). See {@link KrakenOrderRouter}.
 *
 * <p>Promotion path: PAPER → SHADOW or LIVE → LIVE. Rollback to PAPER or
 * DISABLED is always allowed. See {@link KrakenStrategyLifecycleService}.
 */
public enum StrategyMode {

    /** Signals open simulated positions only. The default for every strategy. */
    PAPER,

    /** Signals open a real Kraken order AND a paper position, tagged for comparison. */
    SHADOW,

    /** Signals open real Kraken orders only. */
    LIVE,

    /** No new signals; stale open positions are force-closed like any excluded pair. */
    DISABLED;

    public boolean opensPaper() {
        return this == PAPER || this == SHADOW;
    }

    public boolean opensLive() {
        return this == SHADOW || this == LIVE;
    }
}
