package nu.itark.frosk.strategies.rules;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TimeStopRule} (S10 time-stop with profit condition).
 */
public class TestJTimeStopRule {

    // Flat series at 100 except where a test needs a specific close; entry at index 0 @100.
    private static BarSeries series(double... closes) {
        BarSeries s = new BaseBarSeriesBuilder().withName("T").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.parse("2020-01-01T00:00Z");
        for (int i = 0; i < closes.length; i++) {
            double c = closes[i];
            s.addBar(t.plusDays(i), c, c, c, c, 1d);
        }
        return s;
    }

    private static TradingRecord openAt(int index, double price) {
        TradingRecord r = new BaseTradingRecord();
        r.enter(index, DoubleNum.valueOf(price), DoubleNum.valueOf(1));
        return r;
    }

    @Test
    public void notSatisfiedBeforeTheHoldingWindow() {
        double[] closes = new double[25];
        for (int i = 0; i < closes.length; i++) closes[i] = 100; // never profitable
        TimeStopRule rule = new TimeStopRule(new ClosePriceIndicator(series(closes)), 20, 5.0);
        TradingRecord tr = openAt(0, 100);
        assertFalse(rule.isSatisfied(10, tr), "only 10 bars held — below the 20-bar window");
        assertFalse(rule.isSatisfied(19, tr), "19 bars held — still below the window");
    }

    @Test
    public void satisfiedAfterWindowWhenUnderProfitTarget() {
        double[] closes = new double[25];
        for (int i = 0; i < closes.length; i++) closes[i] = 102; // +2% only, below +5%
        closes[0] = 100;
        TimeStopRule rule = new TimeStopRule(new ClosePriceIndicator(series(closes)), 20, 5.0);
        assertTrue(rule.isSatisfied(20, openAt(0, 100)), "20 bars held and only +2% — time-stop fires");
    }

    @Test
    public void notSatisfiedWhenProfitTargetReached() {
        double[] closes = new double[25];
        for (int i = 0; i < closes.length; i++) closes[i] = 108; // +8%, above +5%
        closes[0] = 100;
        TimeStopRule rule = new TimeStopRule(new ClosePriceIndicator(series(closes)), 20, 5.0);
        assertFalse(rule.isSatisfied(20, openAt(0, 100)), "+8% exceeds target — winner is left alone");
    }

    @Test
    public void notSatisfiedWithoutOpenPosition() {
        TimeStopRule rule = new TimeStopRule(new ClosePriceIndicator(series(100, 100, 100)), 20, 5.0);
        assertFalse(rule.isSatisfied(2, null), "no record");
        assertFalse(rule.isSatisfied(2, new BaseTradingRecord()), "no open position");
    }
}
