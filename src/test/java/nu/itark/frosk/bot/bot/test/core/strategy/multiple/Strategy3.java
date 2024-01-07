package nu.itark.frosk.bot.bot.test.core.strategy.multiple;

import lombok.Getter;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.strategy.CassandreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.BTC_USDT;
import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.ETH_USDT;


/**
 * Strategy 3.
 */
@SuppressWarnings("unused")
@CassandreStrategy(
        strategyId = "03",
        strategyName = "Strategy 3")
@ConditionalOnProperty(
        value = Strategy3.PARAMETER_STRATEGY_3_ENABLED,
        havingValue = "true")
@Getter
public class Strategy3 extends Strategy {

    /** Strategy enabled parameter. */
    public static final String PARAMETER_STRATEGY_3_ENABLED = "strategy3.enabled";

    @Override
    public final Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        return Stream.of(BTC_USDT, ETH_USDT).collect(Collectors.toSet());
    }

}
