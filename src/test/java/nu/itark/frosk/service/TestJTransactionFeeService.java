package nu.itark.frosk.service;

import nu.itark.frosk.strategies.CryptoIntradayStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain unit test, no Spring context. Covers the {@code trading.fee.roundtrip.pct}
 * profile override: CryptoIntradayStrategy-tagged strategies (shared, unmodified,
 * between the crypto and kraken-futures profiles) must resolve to each exchange's
 * own round-trip fee, not always Coinbase's — see class javadoc on TransactionFeeService.
 */
public class TestJTransactionFeeService {

    private static final String CRYPTO_STRATEGY_NAME = FakeCryptoIntradayStrategy.class.getSimpleName();

    /** Stands in for a real CryptoIntradayStrategy bean — only its class name matters here. */
    private static class FakeCryptoIntradayStrategy implements CryptoIntradayStrategy {
        @Override
        public Strategy buildStrategy(BarSeries series) {
            return null;
        }
    }

    private TransactionFeeService newService(Double roundTripOverride) {
        TransactionFeeService service = new TransactionFeeService();
        ReflectionTestUtils.setField(service, "feePerTradePercent", 0.002);
        ReflectionTestUtils.setField(service, "intradayFeePerTradePercent", 0.0003);
        ReflectionTestUtils.setField(service, "cryptoTakerFeePerTradePercent", 0.001);
        ReflectionTestUtils.setField(service, "futuresFeePerTradePercent", 0.0005);
        ReflectionTestUtils.setField(service, "tradingFeeRoundTripPct", roundTripOverride);
        ReflectionTestUtils.setField(service, "cryptoIntradayStrategies", List.of(new FakeCryptoIntradayStrategy()));
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    @Test
    void noOverride_fallsBackToCryptoTakerFeePerTradePercent() {
        TransactionFeeService service = newService(null);

        assertEquals(0.001, service.resolveFeeFraction(CRYPTO_STRATEGY_NAME), 1e-9);
        assertEquals(0.2, service.resolveRoundTripPercent(CRYPTO_STRATEGY_NAME), 1e-9);
    }

    @Test
    void krakenFuturesOverride_0_001RoundTrip_halvesToPerLegFee() {
        TransactionFeeService service = newService(0.001);

        assertEquals(0.0005, service.resolveFeeFraction(CRYPTO_STRATEGY_NAME), 1e-9);
        assertEquals(0.1, service.resolveRoundTripPercent(CRYPTO_STRATEGY_NAME), 1e-9);
    }

    @Test
    void cryptoOverride_0_002RoundTrip_matchesPreExistingDefaultBehavior() {
        TransactionFeeService service = newService(0.002);

        assertEquals(0.001, service.resolveFeeFraction(CRYPTO_STRATEGY_NAME), 1e-9);
        assertEquals(0.2, service.resolveRoundTripPercent(CRYPTO_STRATEGY_NAME), 1e-9);
    }

    @Test
    void override_doesNotAffectNonCryptoStrategies() {
        TransactionFeeService service = newService(0.001);

        // Falls through to the base equity fee — untouched by the crypto-only override.
        assertEquals(0.002, service.resolveFeeFraction("ShortTermMomentumLongTermStrengthStrategy"), 1e-9);
        // Futures strategies resolve via their own constant — also untouched.
        assertEquals(0.0005, service.resolveFeeFraction("TrendFollowingStrategy"), 1e-9);
    }

    @Test
    void isCryptoStrategy_reflectsTheAutowiredCryptoIntradayStrategyBeans() {
        TransactionFeeService service = newService(0.001);

        assertEquals(true, service.isCryptoStrategy(CRYPTO_STRATEGY_NAME));
        assertEquals(false, service.isCryptoStrategy("ShortTermMomentumLongTermStrengthStrategy"));
    }
}
