package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the risk polarity of {@link SP500Strategy}.
 *
 * <p>HedgeIndexService maps a strategy's SELL (exit) to risk=TRUE and its BUY (entry) to
 * risk=FALSE. "S&P 500 below its 200-day MA" is the risk-OFF state, so the strategy must
 * <em>enter</em> (BUY, risk-on) when price recovers ABOVE the 200-day MA and <em>exit</em>
 * (SELL, risk-off) when price drops BELOW it.
 *
 * <p>This test fails if the entry/exit rules are ever swapped back to the original inverted
 * form, which recorded the S&P rallying above its 200-day MA as risk-off.
 */
public class TestJSP500StrategyPolarity {

    @Test
    public void entersAbove200MaExitsBelow() {
        BarSeries series = buildCrossingSeries();

        BarSeriesService barSeriesService = mock(BarSeriesService.class);
        when(barSeriesService.getDataSet("^GSPC", false, false)).thenReturn(series);

        SP500Strategy sp500 = new SP500Strategy(barSeriesService, null);
        Strategy strategy = sp500.buildStrategy();

        TradingRecord record = new BarSeriesManager(series).run(strategy);

        assertFalse(record.getPositions().isEmpty(), "expected at least one closed position");

        // Mid-level is 100; above-MA bars close at 130, below-MA bars at 70.
        for (Position p : record.getPositions()) {
            double entryClose = series.getBar(p.getEntry().getIndex()).getClosePrice().doubleValue();
            double exitClose = series.getBar(p.getExit().getIndex()).getClosePrice().doubleValue();
            assertTrue(entryClose > 100,
                    "entry (BUY / risk-on) must occur ABOVE the 200-day MA, was close=" + entryClose);
            assertTrue(exitClose < 100,
                    "exit (SELL / risk-off) must occur BELOW the 200-day MA, was close=" + exitClose);
        }
    }

    /**
     * 250 flat warm-up bars at 100 (so SMA(200) settles ~100), then clean oscillations well
     * above (130) and below (70) that produce two closed positions crossing the 200-day MA.
     */
    private static BarSeries buildCrossingSeries() {
        BarSeries series = new BaseBarSeriesBuilder().withName("^GSPC").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.parse("2020-01-01T00:00Z");
        double[] prices = new double[250];
        for (int i = 0; i < 200; i++) prices[i] = 100;      // warm-up
        for (int i = 200; i < 210; i++) prices[i] = 70;     // below
        for (int i = 210; i < 220; i++) prices[i] = 130;    // cross up  -> enter
        for (int i = 220; i < 230; i++) prices[i] = 70;     // cross down -> exit
        for (int i = 230; i < 240; i++) prices[i] = 130;    // cross up  -> enter
        for (int i = 240; i < 250; i++) prices[i] = 70;     // cross down -> exit

        for (int i = 0; i < prices.length; i++) {
            double p = prices[i];
            series.addBar(t.plusDays(i), p, p, p, p, 1d);
        }
        return series;
    }
}
