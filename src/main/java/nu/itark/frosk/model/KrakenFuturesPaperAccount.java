package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single-row account tracking simulated Kraken Futures paper trading.
 *
 * <p>Unlike spot paper trading, this account tracks collateral (margin) rather
 * than EUR cash. The {@code collateralUsd} field represents the simulated
 * account balance in USD — Kraken Futures is USD-denominated. All positions
 * are funded from this collateral, and funding payments are deducted from it
 * every 8 hours by {@link nu.itark.frosk.service.KrakenFuturesPaperTradingService}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kraken_futures_paper_account")
public class KrakenFuturesPaperAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "init_collateral_usd", precision = 14, scale = 4, nullable = false)
    private BigDecimal initCollateralUsd;

    /** Available collateral for new positions (USD). */
    @Column(name = "collateral_usd", precision = 14, scale = 4, nullable = false)
    private BigDecimal collateralUsd;

    /** Total realized PnL in USD (sum of all closed positions, net of fees and funding). */
    @Column(name = "realized_pnl_usd", precision = 14, scale = 4, nullable = false)
    private BigDecimal realizedPnlUsd = BigDecimal.ZERO;

    /** Total funding paid (debited) across all positions, USD. Tracked separately for analysis. */
    @Column(name = "total_funding_paid_usd", precision = 14, scale = 4, nullable = false)
    private BigDecimal totalFundingPaidUsd = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
