package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.KrakenStrategyConfig;
import nu.itark.frosk.repo.KrakenStrategyConfigRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes a strategy's {@link StrategyMode}.
 *
 * <p>Read on every (strategy, ticker) pair of every 15m cycle, so modes are
 * cached; {@link #setMode} is the only writer and refreshes the cache, which
 * keeps it consistent within this single process.
 */
@Service
@Profile("kraken-futures")
@RequiredArgsConstructor
public class KrakenStrategyModeService {

    private final KrakenStrategyConfigRepository configRepository;

    private final Map<String, StrategyMode> cache = new ConcurrentHashMap<>();

    /** The strategy's mode; PAPER when it has never been configured. */
    public StrategyMode getMode(String strategyName) {
        return cache.computeIfAbsent(strategyName, name -> configRepository.findById(name)
                .map(KrakenStrategyConfig::getMode)
                .orElse(StrategyMode.PAPER));
    }

    StrategyMode setMode(String strategyName, StrategyMode mode) {
        KrakenStrategyConfig config = configRepository.findById(strategyName)
                .orElseGet(() -> new KrakenStrategyConfig(strategyName, StrategyMode.PAPER));
        StrategyMode previous = config.getMode();
        config.setMode(mode);
        config.setModeChangedAt(LocalDateTime.now());
        configRepository.save(config);
        cache.put(strategyName, mode);
        return previous;
    }
}
