package nu.itark.frosk.analysis;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecurityDTO {
    private String name;
    private BigDecimal oneDayPercent = new BigDecimal(1.2);
    private BigDecimal oneWeekPercent = new BigDecimal(4.5);
    private Double oneMonthPercent = 12.5;
    private Double threeMonthPercent = 23.0;
    private Double sixMonthPercent = 44.0;

    public SecurityDTO(String name)   {
        this.name = name;
    }

}
