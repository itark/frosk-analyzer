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
 * Per-strategy execution mode on the crypto (Coinbase) process.
 *
 * <p>Mirrors {@link KrakenStrategyConfig} exactly — same {@link StrategyMode}
 * enum (PAPER/SHADOW/LIVE/DISABLED is venue-agnostic; reused as-is rather than
 * duplicated), same semantics, separate table because the crypto and
 * kraken-futures processes run against separate H2 database files. Strategies
 * are Spring beans, not database rows, so this table holds the mode instead,
 * keyed by the strategy's simple class name (the same name used in
 * {@code intraday_signal} and {@code crypto_paper_order}). A strategy with no
 * row is PAPER, so nothing needs seeding. Created by {@code ddl-auto=update}
 * like every other table in this project.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coinbase_strategy_config")
public class CoinbaseStrategyConfig {

    @Id
    @Column(name = "strategy_name", length = 80)
    private String strategyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_mode", nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'PAPER'")
    private StrategyMode mode = StrategyMode.PAPER;

    @Column(name = "mode_changed_at")
    private LocalDateTime modeChangedAt;

    public CoinbaseStrategyConfig(String strategyName, StrategyMode mode) {
        this.strategyName = strategyName;
        this.mode = mode;
    }
}
