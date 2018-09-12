package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import lombok.Data;

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Data
@Entity
@Table(name = "featured_strategy", uniqueConstraints={@UniqueConstraint(columnNames={"name", "security"})})
public class FeaturedStrategy implements Comparable<FeaturedStrategy> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "name", unique=true)
	private String name;
	@Column(name = "security", unique=true)
	private String security;
	@Column(name = "total_profit")
	private BigDecimal totalProfit; // Total profit
	@Column(name = "ticks")
	private Integer numberOfTicks; // Number of ticks
	@Column(name = "avg_tick_profit")
	private BigDecimal averageTickProfit; // Average profit (per tick)
	@Column(name = "trades")
	private Integer numberofTrades; // Number of trades
	@Column(name = "prof_trade_ratio")
	private BigDecimal profitableTradesRatio; // "Profitable trades ratio
	@Column(name = "max_dd")
	private BigDecimal maxDD; // "Maximum drawdown
	@Column(name = "rew_risk_ratio")
	private BigDecimal rewardRiskRatio; // Reward-risk ratio
	@Column(name = "transaction_cost")
	private BigDecimal totalTransactionCost; // Total transaction cost (from
											// $1000)
	@Column(name = "buy_hold")
	private BigDecimal buyAndHold; // Buy-and-hold:
	@Column(name = "buy_vs_hold")
	private BigDecimal totalProfitVsButAndHold; // Custom strategy profit vs
												// buy-and-hold strategy profit
	@Column(name = "period")
	private String period;
	@Column(name = "latest_trade")
	@Temporal(TemporalType.TIMESTAMP)
	private Date latestTrade;

	protected FeaturedStrategy () {}

	public FeaturedStrategy(String name, String security, BigDecimal totalProfit, Integer numberOfTicks,
			BigDecimal averageTickProfit, Integer numberofTrades, BigDecimal profitableTradesRatio, BigDecimal maxDD,
			BigDecimal rewardRiskRatio, BigDecimal totalTransactionCost, BigDecimal buyAndHold, BigDecimal totalProfitVsButAndHold, String period, Date latestTrade ) {
		this.name = name;
		this.security = security;
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
	
	
	
	@Override
	public int compareTo(FeaturedStrategy o) {
		return totalProfit.compareTo(o.getTotalProfit());
	}


}
