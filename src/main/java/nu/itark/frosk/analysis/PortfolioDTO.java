package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PortfolioDTO {
    private long id;
    private String snapshotDate;
    private int openPositionCount;
    private BigDecimal totalPnlPercent;
    private List<PortfolioPositionDTO> positions;
}
