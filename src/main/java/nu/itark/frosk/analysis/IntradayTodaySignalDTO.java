package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntradayTodaySignalDTO {
    private String strategyName;
    private String securityName;
    private String type;
    private BigDecimal price;
    private BigDecimal currentPrice;
    private String date;
    /** True while this strategy is still accumulating forward data for a pre-registered test — see DataController.PRE_REGISTRATION_PENDING_STRATEGIES. */
    private boolean preRegistrationPending;
    /** Rule-based confidence tier of the entry signal (crypto only) — see ISignalStrength. */
    private String signalStrength;
    /** Historical stats for this (strategy, security) pair, from FeaturedStrategy — context, not a per-signal score. */
    private BigDecimal historicalWinRate;
    private BigDecimal historicalSqn;
    private Integer historicalTrades;
}
