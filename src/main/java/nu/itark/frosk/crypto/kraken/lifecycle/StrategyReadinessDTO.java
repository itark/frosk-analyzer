package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Promotion readiness for one Kraken strategy — the body of
 * {@code GET /kraken/strategy/{name}/readiness}, and of the 422 a refused
 * promotion returns (so the caller sees exactly which criterion failed).
 *
 * <p>Metrics are computed from the strategy's closed PAPER/SHADOW positions in
 * {@code kraken_futures_paper_order}; see {@link StrategyMetrics} for the exact
 * definitions. Metric fields are null when undefined (e.g. Sharpe with fewer
 * than two trades), never a misleading 0.
 */
@Data
@Builder
public class StrategyReadinessDTO {

    /** Realized performance within one market regime. */
    @Data
    @Builder
    public static class RegimeStats {
        /** RANGING / TRENDING_UP / TRENDING_DOWN, or UNKNOWN for unlabelled trades. */
        private String regime;
        private int closedTrades;
        private BigDecimal totalPnlUsd;
        /** Fraction 0-1. */
        private BigDecimal winRate;
    }


    private String strategyName;
    private StrategyMode mode;
    /** When the mode last changed, {@code yyyy-MM-dd HH:mm}; null if never configured. */
    private String modeChangedAt;

    private int closedTrades;
    private int daysRunning;
    private BigDecimal maxDrawdownPct;
    private BigDecimal sharpeRatio;
    /** Fraction 0–1. */
    private BigDecimal winRate;
    /** Average win / |average loss|. */
    private BigDecimal avgRR;
    private BigDecimal totalPnlUsd;

    private boolean readyForPromotion;
    /** Human-readable reasons promotion is refused — empty when ready. */
    private List<String> blockers;

    /** The thresholds the blockers are measured against, for progress bars. */
    private int minClosedTrades;
    private int minDaysRunning;
    private BigDecimal maxAllowedDrawdownPct;

    /**
     * Closed trades split by the market regime they were opened in — pure
     * instrumentation for judging whether a regime filter would help. Empty for
     * trades predating the regime logging.
     */
    private List<RegimeStats> byRegime;

    /** False when the strategy's property switch keeps it from signalling at all. */
    private boolean enabledByConfig;
    /** In a pre-registered experiment (~/itark/PREREG_*.md) that has not reached its data gate. */
    private boolean preRegistrationPending;
    /**
     * The global {@code crypto.live.trading.enabled} kill switch. While false,
     * SHADOW/LIVE strategies place no real orders — a LIVE strategy then does
     * nothing at all.
     */
    private boolean liveTradingEnabled;
}
