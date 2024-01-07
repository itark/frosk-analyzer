package nu.itark.frosk.bot.bot.test.util.junit;

import nu.itark.frosk.bot.bot.batch.*;
import nu.itark.frosk.bot.bot.repository.OrderRepository;
import nu.itark.frosk.bot.bot.repository.PositionRepository;
import nu.itark.frosk.bot.bot.repository.TradeRepository;
import nu.itark.frosk.bot.bot.service.*;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseProTradeService;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.math.BigDecimal.ZERO;
import static org.knowm.xchange.dto.marketdata.Trades.TradeSortType.SortByTimestamp;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Base mock.
 */
public class BaseMock extends BaseTest {

    /** Default service rate. */
    private static final int DEFAULT_SERVICE_RATE = 900;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected TradeRepository tradeRepository;

    @Autowired
    protected PositionRepository positionRepository;

    @Bean
    @Primary
    public AccountFlux accountFlux() {
        return new AccountFlux(userService());
    }

    @Bean
    @Primary
    public TickerFlux tickerFlux() {
        return new TickerFlux(applicationContext, marketService());
    }

    @Bean
    @Primary
    public OrderFlux orderFlux() {
        return new OrderFlux(orderRepository, tradeService());
    }

    @Bean
    @Primary
    public TradeFlux tradeFlux() {
        return new TradeFlux(orderRepository,tradeRepository, tradeService());
    }

    @Bean
    @Primary
    public PositionFlux positionFlux() {
        return new PositionFlux(positionRepository);
    }

    @Bean
    @Primary
    public MarketService marketService() {
        MarketDataService mock;
        try {
            mock = getXChangeMarketDataServiceMock();
        } catch (IOException e) {
            logger.error("Impossible to instantiate mocked market service");
            return null;
        }
        return new MarketServiceXChangeImplementation(DEFAULT_SERVICE_RATE, mock);
    }

    @Bean
    @Primary
    public UserService userService() {
        AccountService mock;
        try {
            mock = getXChangeAccountServiceMock();
        } catch (IOException e) {
            logger.error("Impossible to instantiate mocked account service");
            return null;
        }
        return new UserServiceXChangeImplementation(DEFAULT_SERVICE_RATE, mock);
    }

    @Bean
    @Primary
    public nu.itark.frosk.bot.bot.service.TradeService tradeService() {
        CoinbaseProTradeService mock;
        try {
            mock = getXChangeTradeServiceMock();
        } catch (IOException e) {
            logger.error("Impossible to instantiate mocked trade service");
            return null;
        }
        return new TradeServiceXChangeImplementation(DEFAULT_SERVICE_RATE, orderRepository, mock);
    }

    /**
     * Returns mocked XChange account service.
     *
     * @return mocked XChange account service
     */
    @Bean
    @Primary
    public AccountService getXChangeAccountServiceMock() throws IOException {
        final AccountService accountServiceMock = mock(AccountService.class);
        given(accountServiceMock.getAccountInfo()).willReturn(
                getAccountInfoReplyForExchangeConfiguration()
        );
        return accountServiceMock;
    }

    /**
     * Returns mocked XChange market data service.
     *
     * @return mocked XChange market data service.
     */
    @Bean
    @Primary
    public MarketDataService getXChangeMarketDataServiceMock() throws IOException {
        return mock(MarketDataService.class);
    }

    /**
     * Returns mocked XChange trade service.
     *
     * @return mocked XChange trade service
     */
    @Bean
    @Primary
    public CoinbaseProTradeService getXChangeTradeServiceMock() throws IOException {
        final CoinbaseProTradeService mock = mock(CoinbaseProTradeService.class);
/*
        given(mock.getOpenOrders()).willReturn(new OpenOrders(Collections.emptyList()));
        given(mock.getTradeHistory(any())).willReturn(new UserTrades(Collections.emptyList(), SortByTimestamp));
*/
        return mock;
    }


    @Bean
    @Primary
    public PositionService positionService() {
        return new PositionServiceCassandreImplementation(positionRepository, tradeService(), positionFlux());
    }

    /**
     * Returns account information for exchange configuration.
     *
     * @return exchange configuration
     */
    protected final AccountInfo getAccountInfoReplyForExchangeConfiguration() {
        return new AccountInfo(
                new Wallet("trade",
                        "trade",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        ZERO,
                        ZERO));
    }

    /**
     * Util method to return a generated ticker.
     *
     * @param instrument instrument (currency pair)
     * @param value      value for all fields
     * @return ticket
     */
    protected static Ticker getGeneratedTicker(final Instrument instrument, final BigDecimal value) {
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getGeneratedTicker(new Date(), instrument, value);
    }

    /**
     * Util method to return a generated ticker.
     *
     * @param date       date
     * @param instrument instrument (currency pair)
     * @param value      value for all fields
     * @return ticket
     */
    protected static Ticker getGeneratedTicker(Date date, final Instrument instrument, final BigDecimal value) {
        return new Ticker.Builder()
                .instrument(instrument) // currency pair.
                .open(value)            // open.
                .last(value)            // last.
                .bid(value)             // bid.
                .ask(value)             // ask.
                .high(value)            // high.
                .low(value)             // low.
                .vwap(value)            // wmap.
                .volume(value)          // value.
                .quoteVolume(value)     // quote volume.
                .timestamp(date)        // timestamp.
                .bidSize(value)         // bid size.
                .askSize(value)         // ask size.
                .build();
    }

}
