package nu.itark.frosk.strategies;

import nu.itark.frosk.model.AccountType;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.service.LstmSignalFilterService;
import nu.itark.frosk.service.TradingAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Spring context) for the EMA-cross-long signal strength
 * scorer: right at a fresh cross the EMA spread is still near zero (at most
 * one bonus condition — RSI — can fire), while several bars into a sustained
 * trend both bonus conditions fire. {@code cryptoRegimeService} is left null
 * deliberately: {@link nu.itark.frosk.strategies.rules.CryptoRegimeRule}'s
 * constructor only stores the reference, and getSignalStrength never
 * evaluates that rule.
 */
public class TestJCryptoEMACrossLongSignalStrength {

    private static final ZoneId UTC = ZoneId.of("UTC");

    /** buildStrategy()'s setInherentExitRule() needs this — unrelated to signal strength. */
    private static TradingAccountService stubTradingAccountService() {
        AccountType accountType = new AccountType();
        accountType.setInherentExitRule(false);
        TradingAccount account = new TradingAccount();
        account.setAccountType(accountType);
        TradingAccountService service = mock(TradingAccountService.class);
        when(service.getActiveTradingAccount()).thenReturn(account);
        return service;
    }

    /**
     * buildStrategy() checks isEnabled() before wiring the LSTM gate — a bare
     * mock's isEnabled() already returns false by default, giving the same
     * disabled/no-op behavior as every non-crypto profile. Unrelated to
     * signal strength, same reasoning as leaving cryptoRegimeService null.
     */
    private static LstmSignalFilterService stubDisabledLstmSignalFilterService() {
        return mock(LstmSignalFilterService.class);
    }

    @Test
    void strengthRisesFromElevatedToStrongAsTheTrendExtends() {
        CryptoEMACrossLongIntradayStrategy strategy = new CryptoEMACrossLongIntradayStrategy();
        ReflectionTestUtils.setField(strategy, "tradingAccountService", stubTradingAccountService());
        ReflectionTestUtils.setField(strategy, "lstmSignalFilterService", stubDisabledLstmSignalFilterService());
        ReflectionTestUtils.setField(strategy, "emaFast", 9);
        ReflectionTestUtils.setField(strategy, "emaSlow", 21);
        ReflectionTestUtils.setField(strategy, "rsiPeriod", 7);
        ReflectionTestUtils.setField(strategy, "maxBarsHeld", 32);

        BarSeries series = new BaseBarSeriesBuilder().withName("test").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, UTC);

        // Flat warmup so EMA(21)/RSI(7) are well-defined before the trend starts.
        double price = 100.0;
        for (int i = 0; i < 25; i++) {
            addBar(series, t.plusMinutes(15L * i), price, price);
        }
        // Sustained uptrend: widens the EMA spread and pushes RSI up bar by bar.
        int trendStart = series.getBarCount();
        for (int i = 0; i < 15; i++) {
            price *= 1.01;
            addBar(series, t.plusMinutes(15L * (25 + i)), price, price);
        }

        strategy.buildStrategy(series);

        SignalStrength early = strategy.getSignalStrength(trendStart + 1);
        SignalStrength late = strategy.getSignalStrength(series.getEndIndex());

        // Early in the trend the EMA spread has not had time to widen past the
        // 0.3% bonus threshold — at most the RSI bonus can be in play yet.
        assertTrue(early == SignalStrength.BASE || early == SignalStrength.ELEVATED,
                "expected BASE or ELEVATED early in the trend, got " + early);
        // After 15 straight up bars both bonus conditions (RSI > 60, EMA spread
        // >= 0.3%) should be met.
        assertEquals(SignalStrength.STRONG, late);
    }

    @Test
    void flatSeriesScoresBase() {
        CryptoEMACrossLongIntradayStrategy strategy = new CryptoEMACrossLongIntradayStrategy();
        ReflectionTestUtils.setField(strategy, "tradingAccountService", stubTradingAccountService());
        ReflectionTestUtils.setField(strategy, "lstmSignalFilterService", stubDisabledLstmSignalFilterService());
        ReflectionTestUtils.setField(strategy, "emaFast", 9);
        ReflectionTestUtils.setField(strategy, "emaSlow", 21);
        ReflectionTestUtils.setField(strategy, "rsiPeriod", 7);
        ReflectionTestUtils.setField(strategy, "maxBarsHeld", 32);

        BarSeries series = new BaseBarSeriesBuilder().withName("test").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, UTC);
        for (int i = 0; i < 30; i++) {
            addBar(series, t.plusMinutes(15L * i), 100.0, 100.0);
        }

        strategy.buildStrategy(series);

        assertEquals(SignalStrength.BASE, strategy.getSignalStrength(series.getEndIndex()));
    }

    private void addBar(BarSeries series, ZonedDateTime endTime, double open, double close) {
        series.addBar(Duration.ofMinutes(15), endTime, open, Math.max(open, close), Math.min(open, close), close, 100);
    }
}
