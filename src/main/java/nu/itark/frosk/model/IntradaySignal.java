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

    /** True when a real Coinbase order was successfully placed for this signal. */
    @Column(name = "live", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean live = false;

    /**
     * Bid/ask spread as a percentage of mid, captured when the signal fired.
     * Null when the book could not be read, or for non-crypto signals.
     *
     * <p>Both the backtest and the paper account fill at the bar close, which
     * charges no spread at all. A market entry pays the ask and a market exit
     * hits the bid, so the true round trip gives up roughly one full spread on
     * top of the taker fee. Recording it per signal is what makes the realized
     * edge measurable instead of assumed.
     */
    @Column(name = "spread_percent", precision = 10, scale = 6)
    private BigDecimal spreadPercent;

    @Column(name = "best_bid", precision = 20, scale = 10)
    private BigDecimal bestBid;

    @Column(name = "best_ask", precision = 20, scale = 10)
    private BigDecimal bestAsk;

    /**
     * Rule-based confidence tier ("BASE"/"ELEVATED"/"STRONG") at entry time, from
     * {@link nu.itark.frosk.strategies.ISignalStrength}. Null for exit signals,
     * strategies that don't implement the interface, and non-crypto signals.
     */
    /**
     * Daily BTC market regime at signal time (RANGING / TRENDING_UP /
     * TRENDING_DOWN), from {@link nu.itark.frosk.service.CryptoRegimeService}.
     *
     * <p>Pure instrumentation: nothing gates on it. It exists so the question
     * "does this strategy behave differently per regime?" can be answered from
     * recorded data instead of intuition — as of 2026-09-24 every trading day on
     * the Kraken instance had been TRENDING_UP, so there was no basis to judge a
     * regime filter. Null on signals emitted before this field existed, and
     * whenever the regime cannot be computed (no BTC history).
     */
    @Column(name = "market_regime", length = 15)
    private String marketRegime;

    @Column(name = "signal_strength", length = 10)
    private String signalStrength;

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
