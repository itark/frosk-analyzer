package nu.itark.frosk.bot.bot.test.core.strategy.multiple;

import lombok.Getter;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.strategy.CassandreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.BTC_ETH;

/**
 * Strategy 2.
 */
@SuppressWarnings("unused")
@CassandreStrategy(
        strategyId = "02",
        strategyName = "Strategy 2")
@ConditionalOnProperty(
        value = Strategy2.PARAMETER_STRATEGY_2_ENABLED,
        havingValue = "true")
@Getter
public class Strategy2 extends Strategy {

    /** Strategy enabled parameter. */
    public static final String PARAMETER_STRATEGY_2_ENABLED = "strategy2.enabled";

    @Override
    public final Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        return Stream.of(BTC_ETH).collect(Collectors.toSet());
    }

}
