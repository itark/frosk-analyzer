package nu.itark.frosk.bot.bot.test.services.dry.mocks;

import nu.itark.frosk.bot.bot.batch.TickerFlux;
import nu.itark.frosk.bot.bot.dto.market.TickerDTO;
import nu.itark.frosk.bot.bot.service.MarketService;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

import static nu.itark.frosk.bot.bot.test.util.junit.BaseTest.ETH_BTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;


@TestConfiguration
public class TradeServiceDryModeTestMock {

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
                .willReturn(Optional.of(TickerDTO.builder().currencyPair(ETH_BTC)
                        .timestamp(ZonedDateTime.now())
                        .last(new BigDecimal("0.2"))
                        .build())
                );
        return marketService;
    }

}
