package nu.itark.frosk.analysis;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class OpenFeaturedStrategyDTO {
    private String securityName;
    private String securityDesc;
    private String name;
    private BigDecimal openPrice;
    private BigDecimal totalProfit;
    private String openTradeDate;
    private String closeTradeDate;
    private BigDecimal closePrice;
    private BigDecimal sqn;
    private BigDecimal expectency;
    private BigDecimal profitableTradesRatio;
}
