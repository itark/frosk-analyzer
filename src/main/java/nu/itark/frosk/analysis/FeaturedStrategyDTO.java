package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;
import nu.itark.frosk.model.StrategyTrade;

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Data
public class FeaturedStrategyDTO implements Comparable<FeaturedStrategyDTO> {

	private String name;
	private String securityName;
	private BigDecimal totalProfit; // Total profit
	private Integer numberOfTicks; // Number of ticks
	private BigDecimal averageTickProfit; // Average profit (per tick)
	private Integer numberofTrades; // Number of trades
	private String profitableTradesRatio; // "Profitable trades ratio
	private BigDecimal maxDD; // "Maximum drawdown
	private BigDecimal rewardRiskRatio; // Reward-risk ratio
	private BigDecimal totalTranactionCost; // Total transaction cost (from
											// $1000)
	private BigDecimal buyAndHold; // Buy-and-hold:
	private BigDecimal totalProfitVsButAndHold; // Custom strategy profit vs
												// buy-and-hold strategy profit

	private String period;
	private String latestTrade;
	private Set<TradeDTO> trades = Collections.EMPTY_SET;
	private List<IndicatorValueDTO> indicatorValues = Collections.EMPTY_LIST ;

	@Override
	public int compareTo(FeaturedStrategyDTO o) {
		return totalProfit.compareTo(o.getTotalProfit());
	}


}
