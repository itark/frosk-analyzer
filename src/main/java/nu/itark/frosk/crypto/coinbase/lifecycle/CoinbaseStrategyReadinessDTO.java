package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.Builder;
import lombok.Data;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Promotion readiness for one Coinbase strategy — the body of
 * {@code GET /coinbase/strategy/{name}/readiness}, and of the 422 a refused
 * promotion returns (so the caller sees exactly which criterion failed).
 *
 * <p>Mirrors {@code StrategyReadinessDTO} field-for-field, with one
 * difference: PnL fields are named/denominated in EUR, since Coinbase trades
 * against EUR (Kraken Futures trades in USD).
 *
 * <p>Metrics are computed from the strategy's closed PAPER/SHADOW trades in
 * {@code crypto_paper_order} (SELL rows); see
 * {@code nu.itark.frosk.crypto.kraken.lifecycle.StrategyMetrics} for the exact
 * definitions (reused as-is — it operates on dimensionless per-trade returns,
 * so the USD naming in that class is cosmetic). Metric fields are null when
 * undefined (e.g. Sharpe with fewer than two trades), never a misleading 0.
 */
@Data
@Builder
public class CoinbaseStrategyReadinessDTO {

    /** Realized performance within one market regime. */
    @Data
    @Builder
    public static class RegimeStats {
        /** RANGING / TRENDING_UP / TRENDING_DOWN, or UNKNOWN for unlabelled trades. */
        private String regime;
        private int closedTrades;
        private BigDecimal totalPnlEur;
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
    private BigDecimal totalPnlEur;

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
