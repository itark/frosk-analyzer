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

    /** PENDING / FILLED / FAILED / CANCELLED */
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
}
