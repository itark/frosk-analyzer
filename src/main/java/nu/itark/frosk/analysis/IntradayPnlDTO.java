package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class IntradayPnlDTO {
    private String ticker;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal totalPnlPercent;
    private BigDecimal avgPnlPercent;
    private BigDecimal bestTradePercent;
    private BigDecimal worstTradePercent;
    private List<IntradayRoundTripDTO> trades;

    @Data
    @Builder
    public static class IntradayRoundTripDTO {
        private String buyTime;
        private BigDecimal buyPrice;
        private String sellTime;
        private BigDecimal sellPrice;
        private BigDecimal pnlPercent;
    }
}
