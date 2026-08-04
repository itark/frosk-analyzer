package nu.itark.frosk.strategies.rules;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

/**
 * Locates the sweep excursion that a reclaim bar terminates.
 *
 * <p>Shared by {@link LiquiditySweepEntryRule} and {@link LiquiditySweepExitRule} so
 * the entry condition and the stop level can never disagree about which bars formed
 * the sweep.
 *
 * <p>The excursion is the run of bars immediately preceding the reclaim during which
 * price had not yet closed back above the level. Requiring the reclaim within
 * {@code confirmBars} of the FIRST penetration — rather than merely "some bar in the
 * last N was below the level" — is what distinguishes a sweep from a breakdown.
 * Price that dips under PDL and sits there for hours before recovering has not swept
 * liquidity and reversed; it has broken down and retraced, which is a different
 * phenomenon with no reason to revert to PDH.
 */
final class SweepWindow {

    private SweepWindow() {
    }

    /**
     * Index of the first bar of the sweep excursion ending at {@code reclaimIndex},
     * or -1 if there is no valid sweep.
     *
     * @param level      the swept level (PDL)
     * @param dayStart   first bar of the current day; the excursion cannot cross it
     * @param confirmBars maximum bars from first penetration to reclaim
     */
    static int sweepStart(BarSeries series, int reclaimIndex, Num level,
                          int dayStart, int confirmBars) {
        if (level == null || level.isZero()) {
            return -1;
        }
        // The reclaim bar must close above the level.
        if (!series.getBar(reclaimIndex).getClosePrice().isGreaterThan(level)) {
            return -1;
        }
        // Walk back while price had not yet reclaimed the level.
        int first = -1;
        for (int i = reclaimIndex; i >= dayStart; i--) {
            boolean penetrated = series.getBar(i).getLowPrice().isLessThan(level);
            if (i < reclaimIndex
                    && series.getBar(i).getClosePrice().isGreaterThan(level)) {
                break;                       // an earlier reclaim ends this excursion
            }
            if (penetrated) {
                first = i;                   // keep walking; we want the EARLIEST
            }
        }
        if (first < 0) {
            return -1;                       // never penetrated
        }
        return (reclaimIndex - first) <= confirmBars ? first : -1;
    }

    /** Lowest low printed across the excursion, or null when there is no valid sweep. */
    static Num sweepLow(BarSeries series, int reclaimIndex, Num level,
                        int dayStart, int confirmBars) {
        int start = sweepStart(series, reclaimIndex, level, dayStart, confirmBars);
        if (start < 0) {
            return null;
        }
        Num low = null;
        for (int i = start; i <= reclaimIndex; i++) {
            Num l = series.getBar(i).getLowPrice();
            if (low == null || l.isLessThan(low)) {
                low = l;
            }
        }
        return low;
    }
}
