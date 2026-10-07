package nu.itark.frosk.strategies;

import nu.itark.frosk.service.LstmSignalFilterService;
import nu.itark.frosk.service.TradingAccountService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the per-ticker SMA-slope trend filter in
 * {@link CryptoVWAPReversionIntradayStrategy}: the same sharp VWAP dip must be
 * blocked in a falling 24h trend, allowed in a rising one, and always allowed
 * with the filter off.
 */
public class TestJCryptoVWAPTrendFilter {

    private static final int BARS = 130;

    /** Linear drift from {@code from} to {@code to}, ending in a 3-bar dip of the given sizes. */
    private static BarSeries driftThenDip(double from, double to, double... dip) {
        BarSeries s = new BaseBarSeriesBuilder().withName("TREND").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 15, 0, 0, ZoneId.of("UTC"));
        int driftBars = BARS - dip.length;
        double price = from;
        for (int i = 0; i < BARS; i++) {
            if (i < driftBars) {
                price = from + (to - from) * i / (driftBars - 1);
            } else {
                price -= dip[i - driftBars];
            }
            s.addBar(Duration.ofMinutes(15), t, price, price, price, price, 1000);
            t = t.plusMinutes(15);
        }
        return s;
    }

    private static CryptoVWAPReversionIntradayStrategy strategy(boolean trendFilter) {
        CryptoVWAPReversionIntradayStrategy strategy = new CryptoVWAPReversionIntradayStrategy();
        ReflectionTestUtils.setField(strategy, "tradingAccountService",
                Mockito.mock(TradingAccountService.class, Mockito.RETURNS_DEEP_STUBS));
        ReflectionTestUtils.setField(strategy, "lstmSignalFilterService", Mockito.mock(LstmSignalFilterService.class));
        ReflectionTestUtils.setField(strategy, "rsiPeriod", 7);
        ReflectionTestUtils.setField(strategy, "stretchPct", 1.2);
        ReflectionTestUtils.setField(strategy, "rsiEntry", 35.0);
        ReflectionTestUtils.setField(strategy, "stopPct", 2.7);
        ReflectionTestUtils.setField(strategy, "maxBarsHeld", 24);
        ReflectionTestUtils.setField(strategy, "trendFilterEnabled", trendFilter);
        ReflectionTestUtils.setField(strategy, "trendSmaBars", 96);
        ReflectionTestUtils.setField(strategy, "trendSlopeBars", 24);
        return strategy;
    }

    private static boolean entersAtLastBar(BarSeries s, boolean trendFilter) {
        return strategy(trendFilter).buildStrategy(s).shouldEnter(s.getEndIndex(), new BaseTradingRecord());
    }

    @Test
    public void blocksDipInAFallingTrend() {
        BarSeries falling = driftThenDip(110, 101, 1, 1, 2);
        assertTrue(entersAtLastBar(falling, false), "without the filter the dip is a valid VWAP entry");
        assertFalse(entersAtLastBar(falling, true), "a falling 24h SMA must block the entry");
    }

    @Test
    public void allowsDipInARisingTrend() {
        BarSeries rising = driftThenDip(90, 101, 1, 2, 2);
        assertTrue(entersAtLastBar(rising, true), "a rising 24h SMA must let the dip through");
    }
}
