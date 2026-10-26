package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.LstmSignalFilterService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.CryptoVWAPReversionIntradayStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CooldownAfterLossRule}, plus its wiring into
 * {@link CryptoVWAPReversionIntradayStrategy} on a synthetic VWAP dip.
 */
public class TestJCooldownAfterLossRule {

    private static TradingRecord closedTrade(int entryIndex, double entryPrice, int exitIndex, double exitPrice) {
        TradingRecord record = new BaseTradingRecord();
        record.enter(entryIndex, DoubleNum.valueOf(entryPrice), DoubleNum.valueOf(1));
        record.exit(exitIndex, DoubleNum.valueOf(exitPrice), DoubleNum.valueOf(1));
        return record;
    }

    @Test
    public void blocksForCooldownBarsAfterALosingExit() {
        CooldownAfterLossRule rule = new CooldownAfterLossRule(2);
        TradingRecord record = closedTrade(5, 100, 10, 98);
        assertFalse(rule.isSatisfied(10, record), "exit bar itself is inside the cooldown");
        assertFalse(rule.isSatisfied(11, record));
        assertFalse(rule.isSatisfied(12, record), "exit + cooldownBars is still blocked");
        assertTrue(rule.isSatisfied(13, record), "cooldown has expired");
    }

    @Test
    public void neverBlocksAfterAWinningExit() {
        CooldownAfterLossRule rule = new CooldownAfterLossRule(2);
        TradingRecord record = closedTrade(5, 100, 10, 101);
        assertTrue(rule.isSatisfied(11, record));
    }

    @Test
    public void passesWithoutAClosedPosition() {
        CooldownAfterLossRule rule = new CooldownAfterLossRule(2);
        assertTrue(rule.isSatisfied(11, null), "no record means no history to cool down from");
        assertTrue(rule.isSatisfied(11, new BaseTradingRecord()), "empty record");
        TradingRecord open = new BaseTradingRecord();
        open.enter(10, DoubleNum.valueOf(100), DoubleNum.valueOf(1));
        assertTrue(rule.isSatisfied(11, open), "an open position has no exit yet");
    }

    @Test
    public void zeroCooldownDisablesTheRule() {
        TradingRecord record = closedTrade(5, 100, 10, 98);
        assertTrue(new CooldownAfterLossRule(0).isSatisfied(11, record));
    }

    // ── Wiring into CryptoVWAPReversionIntradayStrategy ───────────────────────

    /** 40 flat bars at 100, then a drop to 97: stretched well below the UTC-day VWAP, RSI(7) = 0. */
    private static BarSeries vwapDipSeries() {
        BarSeries s = new BaseBarSeriesBuilder().withName("DIP").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 1, 1, 0, 15, 0, 0, ZoneId.of("UTC"));
        double[] closes = new double[43];
        java.util.Arrays.fill(closes, 100);
        closes[40] = 99;
        closes[41] = 98;
        closes[42] = 97;
        for (double c : closes) {
            s.addBar(Duration.ofMinutes(15), t, c, c, c, c, 1000);
            t = t.plusMinutes(15);
        }
        return s;
    }

    static CryptoVWAPReversionIntradayStrategy vwapStrategy(int cooldownBars) {
        CryptoVWAPReversionIntradayStrategy strategy = new CryptoVWAPReversionIntradayStrategy();
        ReflectionTestUtils.setField(strategy, "tradingAccountService",
                Mockito.mock(TradingAccountService.class, Mockito.RETURNS_DEEP_STUBS));
        ReflectionTestUtils.setField(strategy, "lstmSignalFilterService", Mockito.mock(LstmSignalFilterService.class));
        ReflectionTestUtils.setField(strategy, "rsiPeriod", 7);
        ReflectionTestUtils.setField(strategy, "stretchPct", 1.2);
        ReflectionTestUtils.setField(strategy, "rsiEntry", 35.0);
        ReflectionTestUtils.setField(strategy, "stopPct", 2.7);
        ReflectionTestUtils.setField(strategy, "maxBarsHeld", 24);
        ReflectionTestUtils.setField(strategy, "cooldownBarsAfterLoss", cooldownBars);
        return strategy;
    }

    @Test
    public void vwapStrategyHonoursTheConfiguredCooldown() {
        BarSeries s = vwapDipSeries();
        int last = s.getEndIndex();
        TradingRecord recentLoss = closedTrade(30, 100, last - 1, 98);

        Strategy withCooldown = vwapStrategy(2).buildStrategy(s);
        assertTrue(withCooldown.shouldEnter(last, new BaseTradingRecord()),
                "the dip itself must be a valid VWAP entry");
        assertFalse(withCooldown.shouldEnter(last, recentLoss),
                "a loss one bar ago must block re-entry");

        Strategy disabled = vwapStrategy(0).buildStrategy(s);
        assertTrue(disabled.shouldEnter(last, recentLoss), "cooldown 0 keeps the old behaviour");
    }
}
