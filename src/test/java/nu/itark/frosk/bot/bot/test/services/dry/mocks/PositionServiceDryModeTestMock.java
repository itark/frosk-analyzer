package nu.itark.frosk.bot.bot.test.services.dry.mocks;

import nu.itark.frosk.bot.bot.batch.TickerFlux;
import nu.itark.frosk.bot.bot.service.MarketService;
import nu.itark.frosk.bot.bot.test.util.junit.BaseTest;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class PositionServiceDryModeTestMock extends BaseTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @Primary
    public TickerFlux tickerFlux() {
        return new TickerFlux(applicationContext, marketService());
    }

    @Bean
    @Primary
    public MarketService marketService() {
        // Creates the mock.
        MarketService marketService = mock(MarketService.class);

        // We don't use the getTickers method.
        given(marketService.getTickers(any())).willThrow(new NotAvailableFromExchangeException("Not available during tests"));

        // Replies for ETH / BTC.
        given(marketService.getTicker(ETH_BTC))
                .willReturn(Optional.empty());
        // Replies for ETH / USDT.
        given(marketService.getTicker(ETH_USDT))
                .willReturn(Optional.empty());
        return marketService;
    }

}
