package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit row for every simulated fill against {@link KrakenFuturesPaperAccount}.
 *
 * <p>Supports both long and short directions via the {@code direction} field.
 * Funding payments accumulate in {@code totalFundingPaidUsd} on OPEN rows;
 * {@link nu.itark.frosk.service.KrakenFuturesPaperTradingService} updates this
 * field every 8 hours for all open positions.
 *
 * <p>Position lifecycle:
 * <ul>
 *   <li>OPEN row created when SHRT or BUY signal is emitted</li>
 *   <li>CLOSE row created when COVR or SELL signal is emitted — matched to the
 *       most recent OPEN row for the same (ticker, strategy, direction)</li>
 *   <li>The OPEN row flips to status {@code CLOSED} once matched</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kraken_futures_paper_order")
public class KrakenFuturesPaperOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** Kraken Futures symbol, e.g. "PF_XBTUSD". */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /** LONG or SHORT — the direction of this position. */
    @Column(name = "direction", nullable = false, length = 5)
    private String direction;

    @Column(name = "strategy_name", nullable = false, length = 80)
    private String strategyName;

    /** USD notional allocated to this position (entry cost basis). */
    @Column(name = "usd_amount", precision = 14, scale = 4)
    private BigDecimal usdAmount;

    /**
     * Position size in contracts — on PF_* perpetuals one contract is one unit of
     * the BASE asset (1 BTC on PF_XBTUSD), so this is fractional: scale 8 covers
     * every instrument's precision (the finest in use is 4 decimals, BTC).
     * It was scale 0, which truncated BTC/ETH positions to zero.
     */
    @Column(name = "contracts", precision = 20, scale = 8)
    private BigDecimal contracts;

    /** Entry price (mark price at time of simulated fill), USD. */
    @Column(name = "entry_price", precision = 14, scale = 6)
    private BigDecimal entryPrice;

    /** Exit price — set only on CLOSE rows. */
    @Column(name = "exit_price", precision = 14, scale = 6)
    private BigDecimal exitPrice;

    /** OPEN (position active) / CLOSED (matched by exit signal) */
    @Column(name = "status", nullable = false, length = 8)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Realized PnL in USD for this position, set when status becomes CLOSED.
     * Long PnL = (exitPrice - entryPrice) * contracts
     * Short PnL = (entryPrice - exitPrice) * contracts
     * Both are net of entry + exit fees and total accumulated funding.
     */
    @Column(name = "realized_pnl_usd", precision = 14, scale = 4)
    private BigDecimal realizedPnlUsd;

    /**
     * Total funding payments debited against this position, USD.
     * Updated every 8 hours by the funding scheduler.
     * Positive = cost paid by this position (typical for perpetual longs in bull markets).
     */
    @Column(name = "total_funding_paid_usd", precision = 14, scale = 8)
    private BigDecimal totalFundingPaidUsd = BigDecimal.ZERO;

    /**
     * Rule-based confidence tier ("BASE"/"ELEVATED"/"STRONG") of the entry signal.
     * Null on CLOSE rows and entries without strength metadata.
     */
    @Column(name = "signal_strength", length = 10)
    private String signalStrength;

    /**
     * Strategy mode the position was opened under: {@code PAPER}, or
     * {@code SHADOW} when a real Kraken order was sent for the same signal —
     * the key for live-vs-paper comparison analysis (match on strategy_name,
     * symbol and created_at against live_order rows with the same tag).
     * Null on rows written before strategy modes existed; treat as PAPER.
     */
    @Column(name = "execution_mode", length = 10)
    private String executionMode;

    /**
     * Daily BTC market regime when the position was opened — the same value the
     * triggering signal carries. Recorded here too so realized PnL can be split
     * by regime without joining back to intraday_signal. Instrumentation only.
     */
    @Column(name = "market_regime", length = 15)
    private String marketRegime;

    /**
     * Why the position was closed — {@code SIGNAL} (the strategy's own SELL/COVR)
     * or {@code PROTECTIVE_STOP} ({@link nu.itark.frosk.service.KrakenFuturesPaperTradingService}'s
     * poll-based simulation of the live exchange-side stop, see {@code checkProtectiveStops()}).
     * Null on OPEN rows and on rows written before this field existed.
     * Mirrors {@link LiveOrder#getCloseReason()} so paper and live can be compared directly.
     */
    @Column(name = "close_reason", length = 20)
    private String closeReason;
}
