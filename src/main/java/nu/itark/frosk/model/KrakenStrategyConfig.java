package nu.itark.frosk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;

import java.time.LocalDateTime;

/**
 * Per-strategy execution mode on the kraken-futures process.
 *
 * <p>Strategies are Spring beans, not database rows, so there was no existing
 * table to add {@code strategy_mode} to — this table holds it instead, keyed by
 * the strategy's simple class name (the same name used in {@code intraday_signal}
 * and {@code kraken_futures_paper_order}). A strategy with no row is PAPER, so
 * nothing needs seeding. Created by {@code ddl-auto=update} like every other
 * table in this project.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kraken_strategy_config")
public class KrakenStrategyConfig {

    @Id
    @Column(name = "strategy_name", length = 80)
    private String strategyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_mode", nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'PAPER'")
    private StrategyMode mode = StrategyMode.PAPER;

    @Column(name = "mode_changed_at")
    private LocalDateTime modeChangedAt;

    public KrakenStrategyConfig(String strategyName, StrategyMode mode) {
        this.strategyName = strategyName;
        this.mode = mode;
    }
}
