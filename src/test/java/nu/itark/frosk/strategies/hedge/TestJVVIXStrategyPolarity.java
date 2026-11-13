package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DoubleNum;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the risk polarity of {@link VVIXStrategy}: elevated &amp; rising VVIX is the risk-OFF
 * state, so the strategy must <em>exit</em> (SELL, risk=TRUE) on a spike above the threshold
 * and <em>enter</em> (BUY, risk-on) when it falls back. Fails if the rules are swapped back to
 * the original inverted form, which recorded high VVIX as risk-on.
 */
public class TestJVVIXStrategyPolarity {

    @Test
    public void risksOffOnRisingSpikeNotOnCalm() {
        BarSeries series = buildSpikeSeries();

        BarSeriesService barSeriesService = mock(BarSeriesService.class);
        when(barSeriesService.getDataSet("^VVIX", false, false)).thenReturn(series);

        VVIXStrategy vvix = new VVIXStrategy(barSeriesService);
        Strategy strategy = vvix.buildStrategy();

        TradingRecord record = new BarSeriesManager(series).run(strategy);

        assertFalse(record.getPositions().isEmpty(), "expected at least one closed position");

        // Calm baseline is 100; a rising spike peaks at 140 (> threshold 125).
        for (Position p : record.getPositions()) {
            double exitClose = series.getBar(p.getExit().getIndex()).getClosePrice().doubleValue();
            assertTrue(exitClose > 125,
                    "exit (SELL / risk-off) must occur on an elevated VVIX, was close=" + exitClose);
        }
    }

    private static BarSeries buildSpikeSeries() {
        BarSeries series = new BaseBarSeriesBuilder().withName("^VVIX").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.parse("2020-01-01T00:00Z");
        // calm 100, then a clean rising spike through 125 up to 140 (risk-off), then back to calm.
        double[] prices = {100, 100, 100, 100, 100, 105, 115, 130, 140, 135, 110, 100, 100,
                           100, 105, 118, 132, 140, 138, 112, 100, 100, 100};
        for (int i = 0; i < prices.length; i++) {
            double p = prices[i];
            series.addBar(t.plusDays(i), p, p, p, p, 1d);
        }
        return series;
    }
}
