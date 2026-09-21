package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CryptoPaperAccountDTO {
    private BigDecimal initCapitalEur;
    private BigDecimal cashEur;
    /** Cash + cost basis of open positions (not mark-to-market — same equity definition used for sizing). */
    private BigDecimal equityEur;
    private BigDecimal realizedPnlEur;
    private BigDecimal realizedPnlPercent;
    /** Mark-to-market PnL of currently open positions — (currentPrice - entryPrice) × quantity, signed for shorts. */
    private BigDecimal unrealizedPnlEur;
    private BigDecimal unrealizedPnlPct;
    /** realizedPnlEur + unrealizedPnlEur. */
    private BigDecimal totalPnlEur;
    private BigDecimal totalPnlPct;
    private int openPositionsCount;
    private String updatedAt;
    private List<CryptoPaperPositionDTO> openPositions;
}
