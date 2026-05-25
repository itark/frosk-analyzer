package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntradayOpenPositionDTO {
    private String strategyName;
    private String securityName;
    private BigDecimal entryPrice;
    private String entryTime;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnl;
}
