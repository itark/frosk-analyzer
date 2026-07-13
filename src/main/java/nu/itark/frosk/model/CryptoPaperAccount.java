package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single-row account tracking simulated crypto paper trading.
 *
 * <p>Shared across all {@code CryptoIntradayStrategy} implementations — they
 * compete for the same simulated capital, mirroring the fact that live
 * trading would draw from one real Coinbase account. Initialized once
 * (create-if-missing) with {@code crypto.paper.trading.init.capital.eur} and
 * never reset on restart, unlike the equity-side {@link TradingAccount}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crypto_paper_account")
public class CryptoPaperAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "init_capital_eur", precision = 14, scale = 4, nullable = false)
    private BigDecimal initCapitalEur;

    /** Uninvested EUR cash. Equity = cashEur + open positions' cost basis. */
    @Column(name = "cash_eur", precision = 14, scale = 4, nullable = false)
    private BigDecimal cashEur;

    @Column(name = "realized_pnl_eur", precision = 14, scale = 4, nullable = false)
    private BigDecimal realizedPnlEur = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
