package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.Data;
import nu.itark.frosk.dataset.TradeView;

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Data
public class FeaturedStrategyDTO {

	/**
	 * Potensials: Win trades in % Annul return in % Profit factor ...
	 */

	private String name;
	private String security;
	private BigDecimal totalProfit; // Total profit
	private BigDecimal numberOfTicks; // Number of ticks
	private BigDecimal averageTickProfit; // Average profit (per tick)
	private BigDecimal numberofTrades; // Number of trades
	private String profitableTradesRatio; // "Profitable trades ratio
	private BigDecimal maxDD; // "Maximum drawdown
	private String rewardRiskRatio; // Reward-risk ratio
	private BigDecimal totalTranactionCost; // Total transaction cost (from
											// $1000)
	private BigDecimal buyAndHold; // Buy-and-hold:
	private BigDecimal totalProfitVsButAndHold; // Custom strategy profit vs
												// buy-and-hold strategy profit

	private String periodDescription;
	private ZonedDateTime latesTradeDate;
	private List<TradeView> trades;

/*	
	public String getSecurity() {
		return security;
	}

	public void setSecurity(String security) {
		this.security = security;
	}

	public List<TradeView> getTrades() {
		return trades;
	}

	public void setTrades(List<TradeView> trades) {
		this.trades = trades;
	}

	public String getPeriodDescription() {
		return periodDescription;
	}

	public void setPeriodDescription(String periodDescription) {
		this.periodDescription = periodDescription;
	}

	public BigDecimal getTotalProfit() {
		return totalProfit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfitableTradesRatio() {
		return profitableTradesRatio;
	}

	public void setProfitableTradesRatio(String profitableTradesRatio) {
		this.profitableTradesRatio = profitableTradesRatio;
	}

	public String getRewardRiskRatio() {
		return rewardRiskRatio;
	}

	public void setRewardRiskRatio(String rewardRiskRatio) {
		this.rewardRiskRatio = rewardRiskRatio;
	}

	public void setTotalProfit(BigDecimal totalProfit) {
		this.totalProfit = totalProfit;
	}

	public BigDecimal getNumberOfTicks() {
		return numberOfTicks;
	}

	public void setNumberOfTicks(BigDecimal numberOfTicks) {
		this.numberOfTicks = numberOfTicks;
	}

	public BigDecimal getAverageTickProfit() {
		return averageTickProfit;
	}

	public void setAverageTickProfit(BigDecimal averageTickProfit) {
		this.averageTickProfit = averageTickProfit;
	}

	public BigDecimal getNumberofTrades() {
		return numberofTrades;
	}

	public void setNumberofTrades(BigDecimal numberofTrades) {
		this.numberofTrades = numberofTrades;
	}

	public BigDecimal getMaxDD() {
		return maxDD;
	}

	public void setMaxDD(BigDecimal maxDD) {
		this.maxDD = maxDD;
	}

	public BigDecimal getTotalTranactionCost() {
		return totalTranactionCost;
	}

	public void setTotalTranactionCost(BigDecimal totalTranactionCost) {
		this.totalTranactionCost = totalTranactionCost;
	}

	public BigDecimal getBuyAndHold() {
		return buyAndHold;
	}

	public void setBuyAndHold(BigDecimal buyAndHold) {
		this.buyAndHold = buyAndHold;
	}

	public BigDecimal getTotalProfitVsButAndHold() {
		return totalProfitVsButAndHold;
	}

	public void setTotalProfitVsButAndHold(BigDecimal totalProfitVsButAndHold) {
		this.totalProfitVsButAndHold = totalProfitVsButAndHold;
	}
*/
}
