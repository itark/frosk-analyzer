package nu.itark.frosk.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class SecurityDTO {
    private String name;
    private String desc;
    private Double yoyGrowth;
    private Double beta;
    private Double pegRatio;
    private Double forwardEps;
    private Double forwardPe;
    private Double trailingEps;
    private Double trailingPe;
    private BigDecimal oneDayPercent;
    private BigDecimal oneWeekPercent;
    private BigDecimal oneMonthPercent;
    private BigDecimal threeMonthPercent;
    private BigDecimal sixMonthPercent;
    private String bestStrategy;

}
