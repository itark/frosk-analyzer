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

    @Column(name = "open_position_count")
    private int openPositionCount;

    /** Average unrealized P&L % across all open positions. */
    @Column(name = "total_pnl_percent", precision = 12, scale = 4)
    private BigDecimal totalPnlPercent;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("entryDate ASC")
    private List<PortfolioPosition> positions = new ArrayList<>();

    public Portfolio() {}
}
