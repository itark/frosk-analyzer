package nu.itark.frosk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only audit row for every mode change. A promotion to LIVE is what
 * starts risking real money, so each one records the metrics it was granted on
 * — the question "why was this live?" stays answerable after the numbers move.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kraken_strategy_mode_change")
public class KrakenStrategyModeChange {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "strategy_name", nullable = false, length = 80)
    private String strategyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_mode", nullable = false, length = 10)
    private StrategyMode fromMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_mode", nullable = false, length = 10)
    private StrategyMode toMode;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    // Metrics snapshot at the time of the change.
    @Column(name = "closed_trades")
    private Integer closedTrades;

    @Column(name = "days_running")
    private Integer daysRunning;

    @Column(name = "max_drawdown_pct", precision = 10, scale = 4)
    private BigDecimal maxDrawdownPct;
}
