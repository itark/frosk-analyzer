package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class OpenFeaturedStrategyDTO {
    private String securityName;
    private String name;
    private BigDecimal openPrice;
    private BigDecimal totalProfit;
    private BigDecimal totalGrossReturn;
    private String openTradeDate;
    private BigDecimal closePrice;
}
