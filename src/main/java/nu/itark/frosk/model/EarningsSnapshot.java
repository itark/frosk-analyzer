package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Point-in-time capture of what was knowable about an upcoming earnings report.
 *
 * <p>One row per (security, capture day). The whole value of this table is that it
 * is append-only and stamped with {@code capturedAt}: Yahoo serves only the CURRENT
 * expected date and consensus, overwriting silently as estimates move, and offers no
 * historical calendar. A revision series therefore cannot be reconstructed after the
 * fact — it exists only if captured daily, starting now.
 *
 * <p>This is deliberately the opposite of how {@code security.trailingEps} and
 * {@code security.yoyGrowth} are stored. Those are single current-value columns with
 * no date dimension, which is why every strategy reading them was look-ahead biased:
 * a backtest in 2023 could "see" the 2026 figure. Nothing in this table may ever be
 * updated in place.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "earnings_snapshot",
       indexes = {@Index(name = "idx_es_sec_captured", columnList = "security_id,captured_at"),
                  @Index(name = "idx_es_announcement", columnList = "announcement_date")},
       uniqueConstraints = @UniqueConstraint(columnNames = {"security_id", "captured_on"}))
public class EarningsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "security_id", nullable = false)
    private Long securityId;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    /** Expected report date as reported by Yahoo at capture time. */
    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    /** Yahoo's own flag: true when the date is an estimate rather than confirmed. */
    @Column(name = "date_is_estimate")
    private Boolean dateIsEstimate;

    /** Consensus EPS estimate, and the analyst range around it. */
    @Column(name = "eps_consensus", precision = 18, scale = 6)
    private BigDecimal epsConsensus;

    @Column(name = "eps_low", precision = 18, scale = 6)
    private BigDecimal epsLow;

    @Column(name = "eps_high", precision = 18, scale = 6)
    private BigDecimal epsHigh;

    @Column(name = "revenue_consensus", precision = 24, scale = 2)
    private BigDecimal revenueConsensus;

    /** Close on the capture day — anchors any later return calculation. */
    @Column(name = "close_price", precision = 18, scale = 6)
    private BigDecimal closePrice;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt = Instant.now();

    /** Day-granularity duplicate guard; one capture per security per day. */
    @Column(name = "captured_on", nullable = false)
    private LocalDate capturedOn = LocalDate.now();
}
