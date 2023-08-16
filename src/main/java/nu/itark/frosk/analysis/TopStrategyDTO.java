package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopStrategyDTO {
    private String name;
    private BigDecimal totalProfit;
    private BigDecimal sqn;
    private BigDecimal sqnRaw;
}
