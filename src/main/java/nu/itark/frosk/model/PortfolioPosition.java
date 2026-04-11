package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A single position within a Portfolio snapshot.
 * Sourced from one FeaturedStrategy row where open=true.
 */
@Getter
@Setter
@Entity
@Table(name = "portfolio_position")
public class PortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /** FK back to the source FeaturedStrategy row. */
    @Column(name = "featured_strategy_id")
    private Long featuredStrategyId;

    @Column(name = "security_name")
    private String securityName;

    @Column(name = "security_desc")
    private String securityDesc;

    @Column(name = "strategy_name")
    private String strategyName;

    /** Date of the open BUY trade. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entry_date")
    private Date entryDate;

    /** Price at which the position was entered. */
    @Column(name = "entry_price", precision = 12, scale = 6)
    private BigDecimal entryPrice;

    /** Latest available close price at snapshot time. */
    @Column(name = "latest_price", precision = 12, scale = 6)
    private BigDecimal latestPrice;

    /** (latestPrice - entryPrice) / entryPrice * 100 */
    @Column(name = "unrealized_pnl_percent", precision = 12, scale = 4)
    private BigDecimal unrealizedPnlPercent;

    /** true = position is still open; false = position was closed before snapshot. */
    @Column(name = "open", columnDefinition = "BOOLEAN DEFAULT true")
    private boolean open = true;

    @Column(name = "sqn", precision = 12, scale = 4)
    private BigDecimal sqn;

    @Column(name = "expectency", precision = 12, scale = 4)
    private BigDecimal expectency;

    @Column(name = "profitable_trades_ratio", precision = 12, scale = 4)
    private BigDecimal profitableTradesRatio;

    public PortfolioPosition() {}
}
