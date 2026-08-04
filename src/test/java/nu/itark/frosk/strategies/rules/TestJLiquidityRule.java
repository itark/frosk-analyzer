package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.strategies.indicators.MedianTurnoverIndicator;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The liquidity gate must block entries the position size could not be filled at.
 * Modelled on the real case that motivated it: MTG-A.ST turned over ~8,900 SEK per
 * day while the account trades 20,000 SEK positions — 224% of median daily turnover,
 * a trade the backtest happily filled at the bar close.
 */
public class TestJLiquidityRule {

    private static final int BARS = 60;

    /** Series where every bar trades {@code turnoverPerBar} worth of stock. */
    private BarSeries seriesWithTurnover(double turnoverPerBar) {
        BarSeries series = new BaseBarSeriesBuilder().withName("TEST").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(BARS + 5);
        double price = 100.0;
        for (int i = 0; i < BARS; i++) {
            t = t.plusDays(1);
            double volume = turnoverPerBar / price;
            series.addBar(Duration.ofDays(1), t, price, price, price, price, volume);
        }
        return series;
    }

    /**
     * Nums must come from the series, not from a hardcoded implementation — mixing
     * DecimalNum and DoubleNum throws ClassCastException at comparison time. The
     * strategy builds them with {@code series.numOf(...)} for exactly this reason.
     */
    private boolean satisfied(BarSeries series, double positionValue, double maxPct) {
        MedianTurnoverIndicator median = new MedianTurnoverIndicator(series, BARS);
        LiquidityRule rule = new LiquidityRule(median,
                series.numOf(positionValue), series.numOf(maxPct));
        return rule.isSatisfied(series.getEndIndex(), null);
    }

    private boolean satisfied(double turnoverPerBar, double positionValue, double maxPct) {
        return satisfied(seriesWithTurnover(turnoverPerBar), positionValue, maxPct);
    }

    @Test
    public void blocksPositionLargerThanDailyTurnover() {
        // MTG-A case: 20,000 SEK position, ~8,900 SEK daily turnover
        assertFalse(satisfied(8_900, 20_000, 1.0),
                "a position exceeding daily turnover must never be tradable");
    }

    @Test
    public void blocksPositionAboveTheConfiguredShareOfTurnover() {
        // 20,000 of 1,000,000 = 2.0%, above a 1% limit
        assertFalse(satisfied(1_000_000, 20_000, 1.0));
    }

    @Test
    public void allowsPositionComfortablyInsideTheLimit() {
        // 20,000 of 5,000,000 = 0.4%, inside a 1% limit
        assertTrue(satisfied(5_000_000, 20_000, 1.0));
    }

    @Test
    public void allowsPositionExactlyAtTheLimit() {
        // 20,000 of 2,000,000 = exactly 1.0%
        assertTrue(satisfied(2_000_000, 20_000, 1.0));
    }

    @Test
    public void blocksWhenTurnoverHistoryIsMissing() {
        // Fewer bars than the median window — must fail closed, not assume tradability
        BarSeries series = new BaseBarSeriesBuilder().withName("SHORT").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(10);
        for (int i = 0; i < 5; i++) {
            t = t.plusDays(1);
            series.addBar(Duration.ofDays(1), t, 100, 100, 100, 100, 100_000);
        }
        assertFalse(satisfied(series, 20_000, 1.0),
                "without enough history the gate must block, never guess");
    }

    @Test
    public void medianIgnoresASingleBlockTradeSpike() {
        // 59 quiet days plus one huge block trade: the mean would clear a 1% gate,
        // the median must not — that block is not liquidity available on demand.
        BarSeries series = new BaseBarSeriesBuilder().withName("SPIKE").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(BARS + 5);
        double price = 100.0;
        for (int i = 0; i < BARS; i++) {
            t = t.plusDays(1);
            double turnover = (i == BARS - 1) ? 100_000_000 : 500_000;
            series.addBar(Duration.ofDays(1), t, price, price, price, price, turnover / price);
        }
        assertFalse(satisfied(series, 20_000, 1.0),
                "a one-off block trade must not unlock a name that is otherwise thin");
    }
}
