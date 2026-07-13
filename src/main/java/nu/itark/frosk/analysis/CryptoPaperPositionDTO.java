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
}
