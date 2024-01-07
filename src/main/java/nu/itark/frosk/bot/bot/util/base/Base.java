package nu.itark.frosk.bot.bot.util.base;

import nu.itark.frosk.bot.bot.util.mapper.*;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base.
 */
public abstract class Base {

    /** Logger. */
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    protected static final UtilMapper UTIL_MAPPER = Mappers.getMapper(UtilMapper.class);

    protected static final CurrencyMapper CURRENCY_MAPPER = Mappers.getMapper(CurrencyMapper.class);

    protected static final StrategyMapper STRATEGY_MAPPER = Mappers.getMapper(StrategyMapper.class);

    protected static final AccountMapper ACCOUNT_MAPPER = Mappers.getMapper(AccountMapper.class);

    protected static final CandleMapper CANDLE_MAPPER = Mappers.getMapper(CandleMapper.class);

    protected static final TickerMapper TICKER_MAPPER = Mappers.getMapper(TickerMapper.class);

    protected static final OrderMapper ORDER_MAPPER = Mappers.getMapper(OrderMapper.class);


    protected static final TradeMapper TRADE_MAPPER = Mappers.getMapper(TradeMapper.class);

    protected static final PositionMapper POSITION_MAPPER = Mappers.getMapper(PositionMapper.class);


}
