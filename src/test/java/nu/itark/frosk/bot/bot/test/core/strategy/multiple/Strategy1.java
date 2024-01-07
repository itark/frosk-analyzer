package nu.itark.frosk.bot.bot.test.core.strategy.multiple;

import lombok.Getter;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.strategy.CassandreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.Collections;
import java.util.Set;

import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.BTC_USDT;

/**
 * Strategy 1.
 */
@SuppressWarnings("unused")
@CassandreStrategy(
        strategyId = "01",
        strategyName = "Strategy 1")
@ConditionalOnProperty(
        value = Strategy1.PARAMETER_STRATEGY_1_ENABLED,
        havingValue = "true")
@Getter
public class Strategy1 extends Strategy {

    /** Strategy enabled parameter. */
    public static final String PARAMETER_STRATEGY_1_ENABLED = "strategy1.enabled";

    @Override
    public final Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        return Collections.singleton(BTC_USDT);
    }

}
