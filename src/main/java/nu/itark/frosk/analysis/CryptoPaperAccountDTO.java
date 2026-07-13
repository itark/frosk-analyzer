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
    private int openPositionsCount;
    private String updatedAt;
    private List<CryptoPaperPositionDTO> openPositions;
}
