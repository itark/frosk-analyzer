package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per BUY or SELL signal fired by the
 * {@link nu.itark.frosk.strategies.OMX30IntradayMomentumStrategy}.
 *
 * The signal is emitted when the strategy's entry / exit rule is satisfied
 * on the most-recently-completed 15-minute bar.  These rows are the live
 * output of the Tier-0 intraday pipeline; they are never deleted (they form
 * the audit trail for the human trader's decision log).
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

    /** Ticker symbol, e.g. "^OMX". */
    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    /** Bar start-time at which the signal fired (epoch seconds UTC). */
    @Column(name = "signal_timestamp", nullable = false)
    private long signalTimestamp;

    /** "BUY" or "SELL". */
    @Column(name = "signal_type", nullable = false, length = 4)
    private String signalType;

    /** Close price of the signal bar. */
    @Column(name = "close_price", precision = 14, scale = 6)
    private BigDecimal closePrice;

    /** EMA fast value at signal bar (indicator snapshot). */
    @Column(name = "ema9", precision = 14, scale = 6)
    private BigDecimal ema9;

    /** EMA slow value at signal bar (indicator snapshot). */
    @Column(name = "ema21", precision = 14, scale = 6)
    private BigDecimal ema21;

    /** RSI value at signal bar (indicator snapshot). */
    @Column(name = "rsi7", precision = 8, scale = 4)
    private BigDecimal rsi7;

    /** Wall-clock UTC instant when this row was persisted. */
    @Column(name = "created_at", nullable = false)
    private long createdAt = Instant.now().getEpochSecond();

    public IntradaySignal(String ticker, long signalTimestamp, String signalType,
                          BigDecimal closePrice, BigDecimal ema9, BigDecimal ema21,
                          BigDecimal rsi7) {
        this.ticker          = ticker;
        this.signalTimestamp = signalTimestamp;
        this.signalType      = signalType;
        this.closePrice      = closePrice;
        this.ema9            = ema9;
        this.ema21           = ema21;
        this.rsi7            = rsi7;
        this.createdAt       = Instant.now().getEpochSecond();
    }

    /** Human-readable bar start-time. */
    public Instant getSignalInstant() {
        return Instant.ofEpochSecond(signalTimestamp);
    }
}
