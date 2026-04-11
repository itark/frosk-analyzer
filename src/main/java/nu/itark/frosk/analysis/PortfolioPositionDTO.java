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
    private BigDecimal sqn;
    private BigDecimal expectency;
    private BigDecimal profitableTradesRatio;
}
