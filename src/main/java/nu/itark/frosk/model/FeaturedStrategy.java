package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

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
	private Set<StrategyTrade> strategyTrades;

	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	@OrderBy(clause = "date")
	private Set<StrategyIndicatorValue> indicatorValues;

/*
	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	private Set<Order> orders;
*/


	public FeaturedStrategy () {}


}
