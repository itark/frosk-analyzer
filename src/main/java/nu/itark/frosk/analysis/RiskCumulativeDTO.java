package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RiskCumulativeDTO {
    private String dayDate;
    private Long riskyCount;       // count of indicators with risk = true as of that day
    private Long nonRiskyCount;

}
