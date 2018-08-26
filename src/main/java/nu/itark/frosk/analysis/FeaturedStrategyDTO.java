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
	private String latestTradeDate;
	private List<TradeView> trades;


}
