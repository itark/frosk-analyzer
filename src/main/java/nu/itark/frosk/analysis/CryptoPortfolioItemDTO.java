package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CryptoPortfolioItemDTO {
    private String ticker;
    private String strategyName;
    private BigDecimal entryPrice;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnlPercent;
    private String entryDateTime;
}
