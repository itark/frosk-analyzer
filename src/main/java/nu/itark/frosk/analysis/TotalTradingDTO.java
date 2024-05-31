package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class TotalTradingDTO {
    private Date createDate;
    private String type;
    private BigDecimal initTotalValue;
    private BigDecimal accountValue;
    private BigDecimal securityValue;
    private BigDecimal positionValue;
    private BigDecimal totalReturnPercentage;
    private String inherentExitrule;
}
