package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntradaySignalDTO {
    private String ticker;
    private String signalTime;
    private String signalType;
    private BigDecimal closePrice;
    private BigDecimal ema9;
    private BigDecimal ema21;
    private BigDecimal rsi7;
}
