package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Realized paper P&L per calendar day, built from the closed {@code crypto_paper_order}
 * SELL rows — i.e. what the paper account actually traded, in EUR and net of fees,
 * not what the signal stream would have produced.
 *
 * <p>Days are the server's local calendar days (the same clock that stamps
 * {@code crypto_paper_order.created_at}).
 */
@Data
@Builder
public class CryptoPaperDailyPnlDTO {

    private BigDecimal initCapitalEur;
    /** Sum of the daily realized P&L over the returned window. */
    private BigDecimal windowPnlEur;
    /** windowPnlEur as a percentage of initial capital. */
    private BigDecimal windowPnlPctOfCapital;
    private int windowTrades;
    private int windowWins;
    /** Mark-to-market P&L of positions still open — losers that have not been closed yet. */
    private BigDecimal unrealizedPnlEur;
    private int openPositionsCount;
    /** Oldest first; days without closed trades are omitted. */
    private List<DayDTO> days;
    /** Trades closed today, oldest first. */
    private List<TradeDTO> todayTrades;

    @Data
    @Builder
    public static class DayDTO {
        private String date;
        private int trades;
        private int wins;
        private int losses;
        /** Realized P&L in EUR, net of both taker fees. */
        private BigDecimal pnlEur;
        /** pnlEur as a percentage of initial capital — the day's return on the account. */
        private BigDecimal pnlPctOfCapital;
        /** Mean of per-trade net return on entry notional, in percent. */
        private BigDecimal avgTradePct;
        /** Running sum of pnlEur from the first day of the window. */
        private BigDecimal cumulativePnlEur;
    }

    @Data
    @Builder
    public static class TradeDTO {
        private String ticker;
        private String strategyName;
        private String exitTime;
        private BigDecimal entryPrice;
        private BigDecimal exitPrice;
        private BigDecimal entryEur;
        private BigDecimal pnlEur;
        /** Net return on entry notional, in percent; null on rows written before entry_eur_amount existed. */
        private BigDecimal pnlPct;
        private String executionMode;
    }
}
