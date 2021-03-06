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

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Data
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
											
	@Column(name = "buy_hold")
	private BigDecimal buyAndHold; 

	@Column(name = "buy_vs_hold")
	private BigDecimal totalProfitVsButAndHold; 
												
	@Column(name = "period")
	private String period;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "latest_trade")
	private Date latestTrade;
	
	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	private Set<StrategyTrade> trades;

	@OneToMany(mappedBy = "featuredStrategy", fetch=FetchType.LAZY)
	private Set<StrategyIndicatorValue> indicatorValues;	
	
	public FeaturedStrategy () {}

	public FeaturedStrategy(String name,String securityName, BigDecimal totalProfit, Integer numberOfTicks,
			BigDecimal averageTickProfit, Integer numberofTrades, BigDecimal profitableTradesRatio, BigDecimal maxDD,
			BigDecimal rewardRiskRatio, BigDecimal totalTransactionCost, BigDecimal buyAndHold, BigDecimal totalProfitVsButAndHold, String period, Date latestTrade ) {
		this.name = name;
		this.securityName = securityName;
		this.totalProfit = totalProfit;
		this.numberOfTicks = numberOfTicks;
		this.averageTickProfit = averageTickProfit;
		this.numberofTrades = numberofTrades;
		this.profitableTradesRatio = profitableTradesRatio;
		this.maxDD = maxDD;
		this.rewardRiskRatio = rewardRiskRatio;
		this.totalTransactionCost = totalTransactionCost;
		this.buyAndHold = buyAndHold;
		this.totalProfitVsButAndHold = totalProfitVsButAndHold;
		this.period = period;
		this.latestTrade = latestTrade;

	}	
	
}
