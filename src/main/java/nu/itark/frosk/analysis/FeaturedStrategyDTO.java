package nu.itark.frosk.analysis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Data;

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
	private String icon;
	private BigDecimal totalProfit; // Total profit
	private BigDecimal totalGrossReturn; // in cash
	private Integer numberOfTicks; // Number of ticks
	private BigDecimal averageTickProfit; // Average profit (per tick)
	private Integer numberofTrades; // Number of trades
	private String profitableTradesRatio; // "Profitable trades ratio
	private BigDecimal maxDD; // "Maximum drawdown
	private BigDecimal totalTransactionCost; // Total transaction cost (from// $1000)
	private BigDecimal sqn; // ""System Quality Number"
	private BigDecimal expectancy; // ""System Quality Number"
	private String period;
	private String latestTrade;
	private String isOpen;
	private Set<TradeDTO> trades = Collections.EMPTY_SET;
	private List<IndicatorValueDTO> indicatorValues = Collections.EMPTY_LIST;

	@Override
	public int compareTo(FeaturedStrategyDTO o) {
		return totalProfit.compareTo(o.getTotalProfit());
	}


}
