package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioPositionDTO {
    private String securityName;
    private String securityDesc;
    private String strategyName;
    private String entryDate;
    private BigDecimal entryPrice;
    private BigDecimal latestPrice;
    private BigDecimal unrealizedPnlPercent;
    private boolean open;
    /** True if this position's entry trade was opened on the snapshot's build date (i.e. new this run). */
    private boolean newPosition;
    private BigDecimal sqn;
    private BigDecimal expectency;
    private BigDecimal profitableTradesRatio;
}
