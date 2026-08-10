package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class IntradayPnlDTO {
    private String strategyName;
    private String ticker;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal totalPnlPercent;
    private BigDecimal avgPnlPercent;
    private BigDecimal bestTradePercent;
    private BigDecimal worstTradePercent;
    /** True while this strategy is still accumulating forward data for a pre-registered test — see DataController.PRE_REGISTRATION_PENDING_STRATEGIES. */
    private boolean preRegistrationPending;
    private List<IntradayRoundTripDTO> trades;

    @Data
    @Builder
    public static class IntradayRoundTripDTO {
        private String buyTime;
        private BigDecimal buyPrice;
        private String sellTime;
        private BigDecimal sellPrice;
        private BigDecimal pnlPercent;
        /** True when both legs of this round trip were executed as real Coinbase orders. */
        private boolean live;
    }
}
