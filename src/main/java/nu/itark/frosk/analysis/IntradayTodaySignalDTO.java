package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntradayTodaySignalDTO {
    private String securityName;
    private String type;
    private BigDecimal price;
    private String date;
}
