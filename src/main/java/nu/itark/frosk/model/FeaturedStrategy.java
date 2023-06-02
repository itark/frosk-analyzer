package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OrderBy;

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Getter
@Setter
@Entity
@Table(name = "featured_strategy")
public class FeaturedStrategy { 

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "name")
	private String name;

	@Column(name = "security_name")
	private String securityName;

	@Column(name = "total_profit")
	private BigDecimal totalProfit; 

	@Column(name = "ticks")
	private Integer numberOfTicks; 

	@Column(name = "avg_tick_profit")
	private BigDecimal averageTickProfit;

	@Column(name = "number_of_trades")
	private Integer numberofTrades;

	@Column(name = "prof_trade_ratio")
	private BigDecimal profitableTradesRatio; 

	@Column(name = "max_dd")
	private BigDecimal maxDD; 

	@Column(name = "rew_risk_ratio")
	private BigDecimal rewardRiskRatio; 

	@Column(name = "transaction_cost")
	private BigDecimal totalTransactionCost;

	@Column(name = "sqn")
	private BigDecimal sqn;

	@Column(name = "expectency")
	private BigDecimal expectency;

	@Column(name = "period")
	private String period;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "latest_trade")
	private Date latestTrade;

	@Column(name = "open", columnDefinition="BOOLEAN DEFAULT false")
	private boolean isOpen = false;
	
	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	private Set<StrategyTrade> trades;

	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	@OrderBy(clause = "date")
	private Set<StrategyIndicatorValue> indicatorValues;	
	
	public FeaturedStrategy () {}


}
