package nu.itark.frosk.strategies;

/**
 * Rule-based signal confidence tier, computed at entry time from how many
 * "bonus" conditions a strategy's own entry rule cleared beyond its minimum
 * threshold (e.g. RSI well past the entry level, a breakout well past the
 * minimum range width) — not a statistically calibrated win probability.
 *
 * <p>Deliberately a simple tier count rather than a fitted probability model:
 * most strategies in this project do not yet have enough trades, or
 * persisted per-signal indicator snapshots, to support a calibrated
 * probability without a real overfitting risk. See
 * {@code ~/itark/RESEARCH_intraday_edge_search_2026-08.md} for why that
 * caution is not theoretical in this codebase.
 */
public enum SignalStrength {
    /** Entry conditions cleared their minimum threshold and nothing more. */
    BASE,
    /** Exactly one bonus condition cleared with meaningful margin. */
    ELEVATED,
    /** Two or more bonus conditions cleared with meaningful margin. */
    STRONG
}
