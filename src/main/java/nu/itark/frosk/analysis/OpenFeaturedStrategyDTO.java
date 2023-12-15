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
    private BigDecimal totalProfit;
    private String latestTrade;
    private BigDecimal close;
}
