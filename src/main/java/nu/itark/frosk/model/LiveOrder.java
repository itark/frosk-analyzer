package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit row for every live order sent to Coinbase.
 *
 * <p>Created before the order is placed (status=PENDING), then updated with
 * FILLED/FAILED once the order result is known. SELL rows carry
 * {@code realizedPnlEur} so the daily-loss guard can query it directly.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "live_order")
public class LiveOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "coinbase_order_id", length = 80)
    private String coinbaseOrderId;

    @Column(name = "client_order_id", length = 80)
    private String clientOrderId;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    /** BUY or SELL */
    @Column(name = "side", nullable = false, length = 4)
    private String side;

    @Column(name = "strategy_name", nullable = false, length = 80)
    private String strategyName;

    /** EUR spent (BUY) or EUR notional at order time (SELL). */
    @Column(name = "eur_amount", precision = 14, scale = 4)
    private BigDecimal eurAmount;

    @Column(name = "filled_price", precision = 14, scale = 6)
    private BigDecimal filledPrice;

    @Column(name = "filled_quantity", precision = 18, scale = 8)
    private BigDecimal filledQuantity;

    /**
     * FILLED / FAILED / CANCELLED, plus on Kraken:
     * PENDING (placed, fill not yet confirmed — resolved by the reconciler),
     * UNRESOLVED (no fill found within the reconciler's window — check Kraken manually),
     * CLOSING (entry whose exit is placed but not yet confirmed),
     * CLOSED (entry fully closed by a confirmed exit).
     */
    @Column(name = "status", nullable = false, length = 12)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    /**
     * Realized PnL in EUR for this leg (set only on SELL rows).
     * Negative = loss; used by the daily-loss kill switch.
     */
    @Column(name = "realized_pnl_eur", precision = 14, scale = 4)
    private BigDecimal realizedPnlEur;

    /**
     * Kraken strategy mode the order was sent under ({@code SHADOW} or
     * {@code LIVE}). A SHADOW row has a sibling paper position with the same
     * tag. Null on Coinbase orders and on rows predating strategy modes.
     */
    @Column(name = "execution_mode", length = 10)
    private String executionMode;

    /**
     * On exit rows (SELL/COVR): id of the entry row this exit closes. Needed when
     * the exit's fill is confirmed later by the reconciler, which then has to
     * settle PnL against — and close — that specific entry.
     */
    @Column(name = "entry_order_id")
    private Long entryOrderId;

    /**
     * On entry rows: the Kraken order id of the resting reduce-only stop that
     * protects this position, or null when none is active (never placed, already
     * cancelled by a normal exit, or a non-Kraken broker). The reconciler watches
     * it, since a stop that fires closes the position without the app's knowledge.
     */
    @Column(name = "protective_stop_order_id", length = 80)
    private String protectiveStopOrderId;

    /**
     * Why this exit row closed its entry — {@code SIGNAL} (the strategy's own
     * SELL/COVR, set by {@code LiveOrderExecutor.closeEntry}) or
     * {@code PROTECTIVE_STOP} (the resting exchange stop fired, set by
     * {@code KrakenFuturesLiveOrderReconciler.checkProtectiveStop}). Null on
     * entry rows and on rows written before this field existed. Mirrors
     * {@link KrakenFuturesPaperOrder#getCloseReason()} so live and paper can be
     * compared directly.
     */
    @Column(name = "close_reason", length = 20)
    private String closeReason;
}
