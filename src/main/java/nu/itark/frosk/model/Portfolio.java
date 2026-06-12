package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A point-in-time snapshot of all open (and recently closed) strategy positions.
 * Built from FeaturedStrategy entries where open=true.
 */
@Getter
@Setter
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "snapshot_date", nullable = false)
    private Date snapshotDate;

    @Column(name = "portfolio_type", length = 20)
    private String portfolioType = "DAILY";

    @Column(name = "open_position_count")
    private int openPositionCount;

    /**
     * Daily portfolio: average unrealized P&L % across all open positions.
     * Intraday portfolio: sum of today's realized round-trip P&L (net of fees)
     * plus the unrealized P&L of currently open positions.
     */
    @Column(name = "total_pnl_percent", precision = 12, scale = 4)
    private BigDecimal totalPnlPercent;

    /** Sum of today's closed round-trip P&L % net of fees (intraday only). */
    @Column(name = "realized_pnl_percent", precision = 12, scale = 4)
    private BigDecimal realizedPnlPercent;

    /** Number of round trips closed today (intraday only). */
    @Column(name = "closed_trade_count")
    private Integer closedTradeCount;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("entryDate ASC")
    private List<PortfolioPosition> positions = new ArrayList<>();

    public Portfolio() {}
}
