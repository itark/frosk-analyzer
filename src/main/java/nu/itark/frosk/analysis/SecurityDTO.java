package nu.itark.frosk.analysis;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecurityDTO {
    private String name;
    private String desc;
    private BigDecimal oneDayPercent;
    private BigDecimal oneWeekPercent;
    private BigDecimal oneMonthPercent;
    private BigDecimal threeMonthPercent;
    private BigDecimal sixMonthPercent;

    private String bestStrategy;

    public SecurityDTO(String name, String desc)   {
        this.name = name;
        this.desc = desc;
    }

}
