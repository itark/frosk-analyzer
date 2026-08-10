package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CryptoPaperPositionDTO {
    private String ticker;
    private String strategyName;
    private BigDecimal eurAmount;
    private BigDecimal filledPrice;
    private BigDecimal filledQuantity;
    private String createdAt;
    private String signalStrength;
    /** Historical stats for this (strategy, ticker) pair, from FeaturedStrategy — context, not a per-signal score. */
    private BigDecimal historicalWinRate;
    private BigDecimal historicalSqn;
    private Integer historicalTrades;
}
