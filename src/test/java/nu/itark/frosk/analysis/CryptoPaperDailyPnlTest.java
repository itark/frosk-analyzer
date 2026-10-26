package nu.itark.frosk.analysis;

import nu.itark.frosk.model.CryptoPaperOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit test — no Spring context. Run with -Dmaven.test.skip=false. */
class CryptoPaperDailyPnlTest {

    private static CryptoPaperOrder sell(String ticker, LocalDateTime at, String pnl, String entryEur) {
        CryptoPaperOrder o = new CryptoPaperOrder();
        o.setTicker(ticker);
        o.setSide("SELL");
        o.setStrategyName("CryptoVWAPReversionIntradayStrategy");
        o.setCreatedAt(at);
        o.setRealizedPnlEur(new BigDecimal(pnl));
        o.setEntryEurAmount(entryEur == null ? null : new BigDecimal(entryEur));
        o.setFilledQuantity(new BigDecimal("2"));
        o.setFilledPrice(new BigDecimal("55"));
        return o;
    }

    @Test
    void aggregatesPerDayInEurAndPercentOfCapital() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        List<CryptoPaperOrder> sells = new ArrayList<>(List.of(
                sell("A-EUR", today.minusDays(1).atTime(10, 0), "2.00", "100"),
                sell("B-EUR", today.minusDays(1).atTime(11, 0), "-1.00", "100"),
                sell("C-EUR", today.atTime(9, 30), "-3.00", "100"),
                sell("D-EUR", today.atTime(8, 15), "1.00", "100")));

        CryptoPaperDailyPnlDTO dto = CryptoPaperDailyPnl.build(
                sells, new BigDecimal("2000"), new BigDecimal("-4.5"), 3, today);

        assertEquals(2, dto.getDays().size());
        CryptoPaperDailyPnlDTO.DayDTO d1 = dto.getDays().get(0);
        assertEquals("2026-10-07", d1.getDate());
        assertEquals(new BigDecimal("1.00"), d1.getPnlEur());
        assertEquals(1, d1.getWins());
        assertEquals(1, d1.getLosses());
        assertEquals(new BigDecimal("0.050"), d1.getPnlPctOfCapital());       // 1 EUR / 2000 EUR
        assertEquals(new BigDecimal("0.500"), d1.getAvgTradePct());            // (2% + -1%) / 2

        CryptoPaperDailyPnlDTO.DayDTO d2 = dto.getDays().get(1);
        assertEquals(new BigDecimal("-2.00"), d2.getPnlEur());
        assertEquals(new BigDecimal("-1.00"), d2.getCumulativePnlEur());       // running sum over the window

        assertEquals(new BigDecimal("-1.00"), dto.getWindowPnlEur());
        assertEquals(4, dto.getWindowTrades());
        assertEquals(2, dto.getWindowWins());
        assertEquals(new BigDecimal("-4.50"), dto.getUnrealizedPnlEur());
        assertEquals(3, dto.getOpenPositionsCount());
    }

    @Test
    void todayTradesAreListedOldestFirstWithDerivedEntryPrice() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        List<CryptoPaperOrder> sells = new ArrayList<>(List.of(
                sell("LATE", today.atTime(12, 0), "1.00", "100"),
                sell("EARLY", today.atTime(8, 0), "-1.00", "100")));

        CryptoPaperDailyPnlDTO dto = CryptoPaperDailyPnl.build(sells, new BigDecimal("2000"), BigDecimal.ZERO, 0, today);

        assertEquals("EARLY", dto.getTodayTrades().get(0).getTicker());
        assertEquals("08:00", dto.getTodayTrades().get(0).getExitTime());
        assertEquals(0, new BigDecimal("50").compareTo(dto.getTodayTrades().get(0).getEntryPrice())); // 100 EUR / qty 2
        assertEquals(new BigDecimal("-1.000"), dto.getTodayTrades().get(0).getPnlPct());
    }

    @Test
    void rowsWithoutEntryNotionalHaveNoPercentButStillCountInEur() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        CryptoPaperDailyPnlDTO dto = CryptoPaperDailyPnl.build(
                List.of(sell("OLD", today.atTime(9, 0), "2.50", null)),
                new BigDecimal("2000"), BigDecimal.ZERO, 0, today);

        assertEquals(new BigDecimal("2.50"), dto.getWindowPnlEur());
        assertNull(dto.getDays().get(0).getAvgTradePct());
        assertNull(dto.getTodayTrades().get(0).getPnlPct());
    }

    @Test
    void emptyWindowGivesZeroes() {
        CryptoPaperDailyPnlDTO dto = CryptoPaperDailyPnl.build(
                List.of(), new BigDecimal("2000"), BigDecimal.ZERO, 0, LocalDate.of(2026, 10, 8));
        assertTrue(dto.getDays().isEmpty());
        assertEquals(0, dto.getWindowTrades());
        assertEquals(new BigDecimal("0.00"), dto.getWindowPnlEur());
    }
}
