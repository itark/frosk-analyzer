package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.PreviousDayExtremeIndicator;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Long entry on a swept and reclaimed previous-day low.
 *
 * <p>Implements §4 of PREREG_liquidity_sweep_15m.md verbatim. Satisfied at the
 * close of the confirming bar when ALL of:
 * <ol>
 *   <li>a bar within the last {@code confirmBars} printed a low below PDL (the sweep)</li>
 *   <li>the current bar CLOSES back above PDL (the reclaim)</li>
 *   <li>price has not traded above PDH at any point in the current day before now —
 *       if it has, the target is already taken and there is no trade left</li>
 *   <li>the previous day's range is at least {@code minRangePct} of PDL — below that
 *       the target sits inside the noise and the cost fraction returns</li>
 *   <li>no setup has already fired for this day (one per product per UTC day)</li>
 * </ol>
 *
 * <p>Deliberately has NO volatility, trend, time-of-day or volume filter. The
 * pre-registration forbids adding one; doing so after seeing results is the
 * overfitting the document exists to prevent.
 */
public class LiquiditySweepEntryRule extends AbstractRule {

    private final BarSeries series;
    private final PreviousDayExtremeIndicator pdh;
    private final PreviousDayExtremeIndicator pdl;
    private final ZoneId zone;
    private final int confirmBars;
    private final double minRangePct;

    public LiquiditySweepEntryRule(BarSeries series, PreviousDayExtremeIndicator pdh,
                                   PreviousDayExtremeIndicator pdl, ZoneId zone,
                                   int confirmBars, double minRangePct) {
        this.series = series;
        this.pdh = pdh;
        this.pdl = pdl;
        this.zone = zone;
        this.confirmBars = confirmBars;
        this.minRangePct = minRangePct;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = evaluate(index);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }

    private boolean evaluate(int index) {
        Num pdhV = pdh.getValue(index);
        Num pdlV = pdl.getValue(index);
        // Zero means no complete previous day — never tradable.
        if (pdhV.isZero() || pdlV.isZero() || !pdhV.isGreaterThan(pdlV)) {
            return false;
        }
        double pdhD = pdhV.doubleValue();
        double pdlD = pdlV.doubleValue();

        // (4) previous-day range screen
        if (100.0 * (pdhD - pdlD) / pdlD < minRangePct) {
            return false;
        }
        // (1)+(2) the sweep excursion must exist and be terminated by this bar's
        // close back above PDL, within confirmBars of the FIRST penetration.
        int firstBarOfDay = firstBarOfDay(index);
        if (SweepWindow.sweepStart(series, index, pdlV, firstBarOfDay, confirmBars) < 0) {
            return false;
        }
        // (3) target must not already be taken today
        for (int i = firstBarOfDay; i <= index; i++) {
            if (series.getBar(i).getHighPrice().isGreaterThanOrEqual(pdhV)) {
                return false;
            }
        }
        // (5) one setup per UTC day: no earlier bar today may already have qualified
        for (int i = firstBarOfDay; i < index; i++) {
            if (SweepWindow.sweepStart(series, i, pdlV, firstBarOfDay, confirmBars) >= 0) {
                return false;
            }
        }
        return true;
    }

    private int firstBarOfDay(int index) {
        LocalDate today = series.getBar(index).getEndTime().withZoneSameInstant(zone).toLocalDate();
        int first = index;
        for (int i = index - 1; i >= 0; i--) {
            if (!series.getBar(i).getEndTime().withZoneSameInstant(zone).toLocalDate().equals(today)) {
                break;
            }
            first = i;
        }
        return first;
    }
}
