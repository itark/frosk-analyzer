package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntradaySignalDTO {
    private String strategyName;
    private String ticker;
    private String signalTime;
    private String signalType;
    private BigDecimal closePrice;
}
