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
    /** Intraday only: sum of today's closed round-trip PnL % net of fees. */
    private BigDecimal realizedPnlPercent;
    /** Intraday only: number of round trips closed today. */
    private Integer closedTradeCount;
    /** HedgeIndex score in effect on snapshotDate — the market-timing read for this snapshot, not necessarily today's. */
    private int hedgeIndexScore;
    private String hedgeIndexRegime;
    private List<PortfolioPositionDTO> positions;
}
