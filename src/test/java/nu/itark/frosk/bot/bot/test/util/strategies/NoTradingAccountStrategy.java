package nu.itark.frosk.bot.bot.test.util.strategies;

import nu.itark.frosk.bot.bot.dto.user.AccountDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.strategy.BasicCassandreStrategy;
import nu.itark.frosk.bot.bot.strategy.CassandreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.ETH_BTC;
import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.ETH_USDT;

/**
 * Strategy with not trading account (used for tests).
 */
@SuppressWarnings("unused")
@CassandreStrategy(strategyName = "Testable strategy without existing trading account")
@ConditionalOnProperty(
        value = NoTradingAccountStrategy.PARAMETER_NO_TRADING_ACCOUNT_STRATEGY_ENABLED,
        havingValue = "true")
public class NoTradingAccountStrategy extends BasicCassandreStrategy {

    /** Strategy without existing account enabled parameter. */
    public static final String PARAMETER_NO_TRADING_ACCOUNT_STRATEGY_ENABLED = "noTradingAccountStrategy.enabled";

    @Override
    public final Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        Set<CurrencyPairDTO> requestedTickers = new LinkedHashSet<>();
        requestedTickers.add(ETH_BTC);
        requestedTickers.add(ETH_USDT);
        return requestedTickers;
    }

    @Override
    public Optional<AccountDTO> getTradeAccount(Set<AccountDTO> accounts) {
        return Optional.empty();
    }

}
