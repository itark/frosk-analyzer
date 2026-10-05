package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit row for every simulated fill against {@link CryptoPaperAccount}.
 *
 * <p>Mirrors {@link LiveOrder}'s shape and status conventions: a BUY row is
 * {@code FILLED} while the position is open, and flips to {@code CLOSED} once
 * matched by a SELL row. There is no PENDING/FAILED state — simulated fills
 * are immediate at the signal's close price.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crypto_paper_order")
public class CryptoPaperOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    /** BUY or SELL */
    @Column(name = "side", nullable = false, length = 4)
    private String side;

    @Column(name = "strategy_name", nullable = false, length = 80)
    private String strategyName;

    /** EUR spent (BUY) or EUR proceeds (SELL). */
    @Column(name = "eur_amount", precision = 14, scale = 4)
    private BigDecimal eurAmount;

    @Column(name = "filled_price", precision = 14, scale = 6)
    private BigDecimal filledPrice;

    @Column(name = "filled_quantity", precision = 18, scale = 8)
    private BigDecimal filledQuantity;

    /** FILLED (open, for BUY) / CLOSED (matched by a SELL) */
    @Column(name = "status", nullable = false, length = 12)
    private String status = "FILLED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Realized PnL in EUR for this leg (set only on SELL rows). */
    @Column(name = "realized_pnl_eur", precision = 14, scale = 4)
    private BigDecimal realizedPnlEur;

    /**
     * Rule-based confidence tier ("BASE"/"ELEVATED"/"STRONG") of the signal that
     * triggered this BUY, from {@link nu.itark.frosk.strategies.ISignalStrength}.
     * Null on SELL rows and on BUYs from strategies that don't implement it.
     */
    @Column(name = "signal_strength", length = 10)
    private String signalStrength;

    /**
     * Strategy mode the position was opened under: {@code PAPER}, or
     * {@code SHADOW} when a real Coinbase order was sent for the same signal —
     * the key for live-vs-paper comparison analysis (match on strategy_name,
     * ticker and created_at against live_order rows with the same tag).
     * Set on BUY rows and carried over to the matching SELL row. Null on rows
     * written before strategy modes existed; treat as PAPER.
     */
    @Column(name = "execution_mode", length = 10)
    private String executionMode;

    /**
     * On SELL rows: the matching BUY row's {@code eurAmount} (entry notional),
     * copied over when the position closes. Needed because {@link #eurAmount}
     * on a SELL row holds exit proceeds, not the entry notional — and the
     * strategy-lifecycle promotion metrics (per-trade return = net PnL / entry
     * notional) need the latter. Null on BUY rows and on SELL rows written
     * before this field existed.
     */
    @Column(name = "entry_eur_amount", precision = 14, scale = 4)
    private BigDecimal entryEurAmount;
}
