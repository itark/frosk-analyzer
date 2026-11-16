package nu.itark.frosk.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJTradingCalendar {

    @Test
    public void halloweenSeasonIsNovToApr() {
        assertTrue(TradingCalendar.isHalloweenSeason(LocalDate.of(2024, 11, 1)));
        assertTrue(TradingCalendar.isHalloweenSeason(LocalDate.of(2025, 4, 30)));
        assertTrue(TradingCalendar.isHalloweenSeason(LocalDate.of(2025, 1, 15)));
        assertFalse(TradingCalendar.isHalloweenSeason(LocalDate.of(2025, 5, 1)));
        assertFalse(TradingCalendar.isHalloweenSeason(LocalDate.of(2025, 10, 31)));
    }

    @Test
    public void reportMonths() {
        for (int m : new int[]{1, 4, 7, 10}) {
            assertTrue(TradingCalendar.isReportMonth(LocalDate.of(2025, m, 10)));
        }
        assertFalse(TradingCalendar.isReportMonth(LocalDate.of(2025, 6, 10)));
    }

    @Test
    public void nordicPaydayTomWindow() {
        assertTrue(TradingCalendar.isNordicPaydayTomWindow(LocalDate.of(2025, 1, 28)));
        assertTrue(TradingCalendar.isNordicPaydayTomWindow(LocalDate.of(2025, 2, 3)));
        assertFalse(TradingCalendar.isNordicPaydayTomWindow(LocalDate.of(2025, 1, 15)));
    }

    @Test
    public void turnOfMonthFirstThreeAndLastTwoWeekdays() {
        // January 2024: Jan 1 = Monday, last two weekdays = Jan 30 (Tue), Jan 31 (Wed).
        assertTrue(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 1)));
        assertTrue(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 3)));
        assertFalse(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 4)), "4th weekday is outside the first-3");
        assertTrue(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 30)));
        assertTrue(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 31)));
        assertFalse(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 15)), "mid-month");
        assertFalse(TradingCalendar.isTurnOfMonthWindow(LocalDate.of(2024, 1, 6)), "Saturday");
    }
}
