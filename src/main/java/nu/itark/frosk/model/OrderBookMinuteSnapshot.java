package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per (security, UTC minute): the top-of-book state at minute end and
 * the accumulated Cont-Kukanov-Stoikov order flow imbalance over that minute.
 *
 * <p>Feeds {@code PREREG_ofi_1m.md} (~/itark) — the per-minute OFI values here
 * are summed over a trailing 15-minute rolling window to produce that
 * pre-registration's primary signal. Do not use this table for anything else
 * without checking whether it changes what that document calls "locked".
 *
 * <p>{@code ofiSum} is the sum of the per-event OFI (see
 * {@code nu.itark.frosk.crypto.coinbase.websocket.OrderBookState}) across
 * every level2 book update received during the minute, not a point sample —
 * the whole reason this exists instead of reusing {@link IntradayBar} is that
 * "average bid/ask over the minute" would destroy the signal, while "sum of
 * flow events over the minute" preserves it.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "order_book_minute_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_order_book_minute",
        columnNames = {"security_id", "minute_timestamp"}
    )
)
public class OrderBookMinuteSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** FK to security.id — not a JPA relation, matching {@link IntradayBar}'s convention. */
    @Column(name = "security_id", nullable = false)
    private Long securityId;

    /** Minute start as Unix epoch seconds (UTC), floored — same rationale as {@link IntradayBar}. */
    @Column(name = "minute_timestamp", nullable = false)
    private long minuteTimestamp;

    @Column(name = "best_bid_price", precision = 20, scale = 10)
    private BigDecimal bestBidPrice;

    @Column(name = "best_bid_size", precision = 20, scale = 10)
    private BigDecimal bestBidSize;

    @Column(name = "best_ask_price", precision = 20, scale = 10)
    private BigDecimal bestAskPrice;

    @Column(name = "best_ask_size", precision = 20, scale = 10)
    private BigDecimal bestAskSize;

    /** Sum of per-event OFI (CKS 2014 top-of-book formula) over this minute. */
    @Column(name = "ofi_sum", precision = 24, scale = 10)
    private BigDecimal ofiSum;

    /** Number of level2 book-update events that contributed to {@code ofiSum} — 0 means a quiet, not missing, minute. */
    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @Column(name = "captured_at", nullable = false)
    private long capturedAt = Instant.now().getEpochSecond();

    public OrderBookMinuteSnapshot(Long securityId, long minuteTimestamp,
                                   BigDecimal bestBidPrice, BigDecimal bestBidSize,
                                   BigDecimal bestAskPrice, BigDecimal bestAskSize,
                                   BigDecimal ofiSum, int eventCount) {
        this.securityId = securityId;
        this.minuteTimestamp = minuteTimestamp;
        this.bestBidPrice = bestBidPrice;
        this.bestBidSize = bestBidSize;
        this.bestAskPrice = bestAskPrice;
        this.bestAskSize = bestAskSize;
        this.ofiSum = ofiSum;
        this.eventCount = eventCount;
        this.capturedAt = Instant.now().getEpochSecond();
    }
}
