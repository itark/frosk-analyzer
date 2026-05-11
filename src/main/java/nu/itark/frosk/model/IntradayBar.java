package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stores one intraday OHLCV bar fetched from Yahoo Finance.
 *
 * Used by the Tier-0 (10-minute) intraday pipeline.  Daily bars continue to
 * live in {@link SecurityPrice}; this table holds only the short-lived
 * intraday window (last 7 trading days, pruned on every sync run).
 *
 * The composite unique constraint on (security_id, bar_timestamp, interval_code)
 * allows safe upsert-style saves — re-fetching the same bar does not create a
 * duplicate row.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "intraday_bar",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_intraday_bar",
        columnNames = {"security_id", "bar_timestamp", "interval_code"}
    )
)
public class IntradayBar {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** FK to security.id — not a JPA relation so we stay lightweight. */
    @Column(name = "security_id", nullable = false)
    private Long securityId;

    /**
     * Bar start-time as Unix epoch seconds (UTC).
     * Stored as a plain long so we avoid timezone ambiguity that plagues
     * {@link java.util.Date} across DST transitions.
     */
    @Column(name = "bar_timestamp", nullable = false)
    private long barTimestamp;

    /**
     * Yahoo Finance granularity string, e.g. "5m", "15m", "1h".
     * Kept short — max 4 chars.
     */
    @Column(name = "interval_code", nullable = false, length = 4)
    private String intervalCode;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal close;

    @Column
    private Long volume;

    public IntradayBar(Long securityId, long barTimestamp, String intervalCode,
                       BigDecimal open, BigDecimal high, BigDecimal low,
                       BigDecimal close, Long volume) {
        this.securityId    = securityId;
        this.barTimestamp  = barTimestamp;
        this.intervalCode  = intervalCode;
        this.open          = open;
        this.high          = high;
        this.low           = low;
        this.close         = close;
        this.volume        = volume;
    }

    /** Convenience — bar start as {@link Instant}. */
    public Instant getInstant() {
        return Instant.ofEpochSecond(barTimestamp);
    }
}
