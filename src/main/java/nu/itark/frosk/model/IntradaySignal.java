package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per BUY or SELL signal fired by an intraday strategy.
 *
 * <p>The signal is emitted when a strategy's entry / exit rule is satisfied
 * on the most-recently-completed bar. These rows are the live output of the
 * Tier-0 intraday pipeline; they are never deleted (they form the audit trail
 * for the human trader's decision log).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "intraday_signal")
public class IntradaySignal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "strategy_name", nullable = false, length = 80)
    private String strategyName;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "signal_timestamp", nullable = false)
    private long signalTimestamp;

    /** "BUY" or "SELL". */
    @Column(name = "signal_type", nullable = false, length = 4)
    private String signalType;

    @Column(name = "close_price", precision = 14, scale = 6)
    private BigDecimal closePrice;

    @Column(name = "created_at", nullable = false)
    private long createdAt = Instant.now().getEpochSecond();

    public IntradaySignal(String strategyName, String ticker, long signalTimestamp,
                          String signalType, BigDecimal closePrice) {
        this.strategyName   = strategyName;
        this.ticker          = ticker;
        this.signalTimestamp = signalTimestamp;
        this.signalType      = signalType;
        this.closePrice      = closePrice;
        this.createdAt       = Instant.now().getEpochSecond();
    }

    public Instant getSignalInstant() {
        return Instant.ofEpochSecond(signalTimestamp);
    }
}
