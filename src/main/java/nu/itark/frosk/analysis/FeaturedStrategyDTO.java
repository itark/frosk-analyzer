package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;
import nu.itark.frosk.dataset.IndicatorValues;
import nu.itark.frosk.dataset.TradeView;

/**
 * This class holds all significant strategies and it values.
 * 
 * @author fredrikmoller
 *
 */
@Data
public class FeaturedStrategyDTO implements Comparable<FeaturedStrategyDTO> {

	private String name;
	private String security;
	private BigDecimal totalProfit; // Total profit
	private Integer numberOfTicks; // Number of ticks
	private BigDecimal averageTickProfit; // Average profit (per tick)
	private Integer numberofTrades; // Number of trades
	private BigDecimal profitableTradesRatio; // "Profitable trades ratio
	private BigDecimal maxDD; // "Maximum drawdown
	private BigDecimal rewardRiskRatio; // Reward-risk ratio
	private BigDecimal totalTranactionCost; // Total transaction cost (from
											// $1000)
	private BigDecimal buyAndHold; // Buy-and-hold:
	private BigDecimal totalProfitVsButAndHold; // Custom strategy profit vs
												// buy-and-hold strategy profit

	private String periodDescription;
	private Date latestTradeDate;
	private List<TradeView> trades;
	private List<IndicatorValues> indicatorValues;

	@Override
	public int compareTo(FeaturedStrategyDTO o) {
		return totalProfit.compareTo(o.getTotalProfit());
	}


}
