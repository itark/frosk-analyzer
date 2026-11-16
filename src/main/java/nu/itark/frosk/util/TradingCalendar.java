package nu.itark.frosk.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calendar helpers for the seasonal / turn-of-month timing strategies
 * (S3, S5, S8 in {@code docs/research/omx-equity-strategies-research.md}).
 *
 * <p>Trading days are approximated as weekdays (Mon–Fri). This ignores public holidays, which
 * can shift a window by a day a few times a year — acceptable for a timing overlay, and it keeps
 * the logic deterministic and testable without a full exchange calendar.
 */
public final class TradingCalendar {

    private TradingCalendar() {}

    /** S3 Halloween ("sell in May"): invested November–April. */
    public static boolean isHalloweenSeason(LocalDate d) {
        int m = d.getMonthValue();
        return m >= 11 || m <= 4;
    }

    /** OMX reporting months (Jan/Apr/Jul/Oct) — pre-report drift window (S8). */
    public static boolean isReportMonth(LocalDate d) {
        int m = d.getMonthValue();
        return m == 1 || m == 4 || m == 7 || m == 10;
    }

    /**
     * S8 Nordic payday-shifted turn-of-month window: Swedish payday is the 25th, which pushes
     * fund inflows to ~the 27th–28th, selling ~the 3rd–4th. Day-of-month based (calendar days).
     */
    public static boolean isNordicPaydayTomWindow(LocalDate d) {
        int day = d.getDayOfMonth();
        return day >= 27 || day <= 4;
    }

    /**
     * S5 turn-of-month window: the last 2 and first 3 <em>trading</em> days (weekdays) of the
     * month. Returns false on weekends.
     */
    public static boolean isTurnOfMonthWindow(LocalDate d) {
        if (isWeekend(d)) {
            return false;
        }
        List<LocalDate> weekdays = weekdaysOfMonth(d.getYear(), d.getMonthValue());
        int idx = weekdays.indexOf(d);
        if (idx < 0) {
            return false;
        }
        return idx < 3 || idx >= weekdays.size() - 2;
    }

    public static boolean isWeekend(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** All weekdays (Mon–Fri) of the given month, in ascending order. */
    public static List<LocalDate> weekdaysOfMonth(int year, int month) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate d = LocalDate.of(year, month, 1);
        int len = d.lengthOfMonth();
        for (int day = 1; day <= len; day++) {
            LocalDate cur = LocalDate.of(year, month, day);
            if (!isWeekend(cur)) {
                out.add(cur);
            }
        }
        return out;
    }
}
