package nu.itark.frosk.analysis;

import nu.itark.frosk.model.CryptoPaperOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure aggregation of closed paper SELL rows into {@link CryptoPaperDailyPnlDTO}.
 * Kept free of Spring so it can be unit-tested without a database.
 */
public final class CryptoPaperDailyPnl {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private CryptoPaperDailyPnl() {}

    /**
     * @param sells         closed paper SELL rows inside the window (any order)
     * @param initCapital   paper account's initial capital in EUR
     * @param unrealizedEur mark-to-market P&L of still-open positions
     * @param openCount     number of still-open positions
     * @param today         the current calendar day, whose trades are listed individually
     */
    public static CryptoPaperDailyPnlDTO build(List<CryptoPaperOrder> sells, BigDecimal initCapital,
                                               BigDecimal unrealizedEur, int openCount, LocalDate today) {
        Map<LocalDate, List<CryptoPaperOrder>> byDay = new TreeMap<>();
        for (CryptoPaperOrder o : sells) {
            if (!"SELL".equals(o.getSide()) || o.getRealizedPnlEur() == null) continue;
            byDay.computeIfAbsent(o.getCreatedAt().toLocalDate(), k -> new ArrayList<>()).add(o);
        }

        List<CryptoPaperDailyPnlDTO.DayDTO> days = new ArrayList<>();
        List<CryptoPaperDailyPnlDTO.TradeDTO> todayTrades = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        int totalTrades = 0;
        int totalWins = 0;

        for (Map.Entry<LocalDate, List<CryptoPaperOrder>> e : byDay.entrySet()) {
            List<CryptoPaperOrder> list = e.getValue();
            list.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            BigDecimal dayPnl = BigDecimal.ZERO;
            BigDecimal pctSum = BigDecimal.ZERO;
            int pctCount = 0;
            int wins = 0;
            int losses = 0;
            for (CryptoPaperOrder o : list) {
                BigDecimal pnl = o.getRealizedPnlEur();
                dayPnl = dayPnl.add(pnl);
                if (pnl.signum() > 0) wins++;
                else if (pnl.signum() < 0) losses++;
                BigDecimal pct = tradePct(o);
                if (pct != null) {
                    pctSum = pctSum.add(pct);
                    pctCount++;
                }
                if (e.getKey().equals(today)) {
                    todayTrades.add(toTrade(o, pct));
                }
            }
            cumulative = cumulative.add(dayPnl);
            totalTrades += list.size();
            totalWins += wins;
            days.add(CryptoPaperDailyPnlDTO.DayDTO.builder()
                    .date(e.getKey().toString())
                    .trades(list.size())
                    .wins(wins)
                    .losses(losses)
                    .pnlEur(scale(dayPnl, 2))
                    .pnlPctOfCapital(pctOf(dayPnl, initCapital))
                    .avgTradePct(pctCount == 0 ? null
                            : pctSum.divide(BigDecimal.valueOf(pctCount), 3, RoundingMode.HALF_UP))
                    .cumulativePnlEur(scale(cumulative, 2))
                    .build());
        }

        return CryptoPaperDailyPnlDTO.builder()
                .initCapitalEur(initCapital)
                .windowPnlEur(scale(cumulative, 2))
                .windowPnlPctOfCapital(pctOf(cumulative, initCapital))
                .windowTrades(totalTrades)
                .windowWins(totalWins)
                .unrealizedPnlEur(scale(unrealizedEur, 2))
                .openPositionsCount(openCount)
                .days(days)
                .todayTrades(todayTrades)
                .build();
    }

    /** Net return on entry notional in percent, or null when the entry notional is unknown. */
    static BigDecimal tradePct(CryptoPaperOrder o) {
        BigDecimal entry = o.getEntryEurAmount();
        if (entry == null || entry.signum() == 0 || o.getRealizedPnlEur() == null) return null;
        return o.getRealizedPnlEur().multiply(HUNDRED).divide(entry, 3, RoundingMode.HALF_UP);
    }

    private static CryptoPaperDailyPnlDTO.TradeDTO toTrade(CryptoPaperOrder o, BigDecimal pct) {
        BigDecimal entryPrice = null;
        if (o.getEntryEurAmount() != null && o.getFilledQuantity() != null && o.getFilledQuantity().signum() != 0) {
            entryPrice = o.getEntryEurAmount().divide(o.getFilledQuantity(), 6, RoundingMode.HALF_UP);
        }
        return CryptoPaperDailyPnlDTO.TradeDTO.builder()
                .ticker(o.getTicker())
                .strategyName(o.getStrategyName())
                .exitTime(o.getCreatedAt().format(HHMM))
                .entryPrice(entryPrice)
                .exitPrice(o.getFilledPrice())
                .entryEur(o.getEntryEurAmount())
                .pnlEur(scale(o.getRealizedPnlEur(), 2))
                .pnlPct(pct)
                .executionMode(o.getExecutionMode())
                .build();
    }

    private static BigDecimal pctOf(BigDecimal amount, BigDecimal base) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        return amount.multiply(HUNDRED).divide(base, 3, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal v, int s) {
        return v == null ? null : v.setScale(s, RoundingMode.HALF_UP);
    }
}
