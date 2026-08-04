package nu.itark.frosk.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.strategies.CryptoIntradayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for the per-trade transaction fee of a given strategy.
 *
 * <p>Crypto trades on Coinbase pay the taker fee, a multiple of the equity intraday
 * broker fee, so the two must never be interchanged. Resolving this in more than
 * one place is how they drifted apart: {@code BarSeriesService} had a
 * hand-maintained list that was missing three of the five crypto strategies, and
 * {@code DataController} deducted the equity fee for every crypto round trip.
 * Both now route through here; the rates themselves live in application.properties
 * and track the account's Coinbase volume tier.
 *
 * <p>The crypto set is therefore derived from the {@link CryptoIntradayStrategy}
 * beans themselves rather than written out by hand — a new crypto strategy is
 * picked up automatically and cannot silently fall back to the equity fee.
 */
@Service
@Slf4j
public class TransactionFeeService {

    @Value("${exchange.transaction.feePerTradePercent}")
    private double feePerTradePercent;

    @Value("${exchange.transaction.intradayFeePerTradePercent:0.0003}")
    private double intradayFeePerTradePercent;

    @Value("${exchange.transaction.cryptoTakerFeePerTradePercent:0.006}")
    private double cryptoTakerFeePerTradePercent;

    /** Unchanged from the original BarSeriesService list — equity fees are not in scope here. */
    private static final Set<String> EQUITY_INTRADAY_STRATEGIES = Set.of(
            "OpeningRangeBreakoutIntradayStrategy",
            "VWAPMeanReversionIntradayStrategy",
            "GapReversalIntradayStrategy"
    );

    @Autowired(required = false)
    private List<CryptoIntradayStrategy> cryptoIntradayStrategies;

    private Set<String> cryptoStrategyNames = Set.of();

    @PostConstruct
    private void init() {
        if (cryptoIntradayStrategies != null) {
            cryptoStrategyNames = cryptoIntradayStrategies.stream()
                    .map(s -> s.getClass().getSimpleName())
                    .collect(Collectors.toSet());
        }
        log.info("TransactionFeeService: crypto taker fee {}% applies to {}",
                cryptoTakerFeePerTradePercent * 100, cryptoStrategyNames);
    }

    /** Per-trade fee as a fraction (0.006 = 0.6%), for one leg of a round trip. */
    public double resolveFeeFraction(String strategyName) {
        if (cryptoStrategyNames.contains(strategyName)) {
            return cryptoTakerFeePerTradePercent;
        }
        if (EQUITY_INTRADAY_STRATEGIES.contains(strategyName)) {
            return intradayFeePerTradePercent;
        }
        return feePerTradePercent;
    }

    /** Round-trip cost in percent (entry + exit), e.g. 1.2 for a crypto taker round trip. */
    public double resolveRoundTripPercent(String strategyName) {
        return 2 * resolveFeeFraction(strategyName) * 100;
    }

    public boolean isCryptoStrategy(String strategyName) {
        return cryptoStrategyNames.contains(strategyName);
    }
}
