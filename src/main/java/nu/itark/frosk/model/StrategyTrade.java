package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import lombok.*;

@Data
//@Builder
@NoArgsConstructor
@Entity
@Table(name = "strategy_trade", uniqueConstraints={@UniqueConstraint(columnNames={"date", "type", "featured_strategy_id"})})
public class StrategyTrade {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;
	
	@Column(name = "price", precision=12, scale=6)
	private BigDecimal price;

	@Column(name = "gross_profit")
	private BigDecimal grossProfit;

	@Column(name = "amount")
	private BigDecimal amount = BigDecimal.ZERO;

	@Column(name = "pnl")
	private BigDecimal pnl;

	@Column(name = "type")
	private String type;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="featured_strategy_id", nullable=false)
    private FeaturedStrategy featuredStrategy;


}
