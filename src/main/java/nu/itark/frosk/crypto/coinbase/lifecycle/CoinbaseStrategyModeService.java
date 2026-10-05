package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import nu.itark.frosk.model.CoinbaseStrategyConfig;
import nu.itark.frosk.repo.CoinbaseStrategyConfigRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes a strategy's {@link StrategyMode} on the crypto (Coinbase)
 * process. Mirrors {@code KrakenStrategyModeService} exactly, against
 * {@code coinbase_strategy_config} instead of {@code kraken_strategy_config}.
 *
 * <p>Read on every (strategy, ticker) pair of every 15m cycle, so modes are
 * cached; {@link #setMode} is the only writer and refreshes the cache, which
 * keeps it consistent within this single process.
 */
@Service
@Profile("crypto")
@RequiredArgsConstructor
public class CoinbaseStrategyModeService {

    private final CoinbaseStrategyConfigRepository configRepository;

    private final Map<String, StrategyMode> cache = new ConcurrentHashMap<>();

    /** The strategy's mode; PAPER when it has never been configured. */
    public StrategyMode getMode(String strategyName) {
        return cache.computeIfAbsent(strategyName, name -> configRepository.findById(name)
                .map(CoinbaseStrategyConfig::getMode)
                .orElse(StrategyMode.PAPER));
    }

    StrategyMode setMode(String strategyName, StrategyMode mode) {
        CoinbaseStrategyConfig config = configRepository.findById(strategyName)
                .orElseGet(() -> new CoinbaseStrategyConfig(strategyName, StrategyMode.PAPER));
        StrategyMode previous = config.getMode();
        config.setMode(mode);
        config.setModeChangedAt(LocalDateTime.now());
        configRepository.save(config);
        cache.put(strategyName, mode);
        return previous;
    }
}
