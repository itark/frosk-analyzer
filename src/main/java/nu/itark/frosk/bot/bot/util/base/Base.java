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

/*

    */
/** Type mapper. *//*

    protected static final UtilMapper UTIL_MAPPER = Mappers.getMapper(UtilMapper.class);

    */
/** Currency mapper. *//*

    protected static final CurrencyMapper CURRENCY_MAPPER = Mappers.getMapper(CurrencyMapper.class);

    */
/** Strategy mapper. *//*

    protected static final StrategyMapper STRATEGY_MAPPER = Mappers.getMapper(StrategyMapper.class);

    */
/** Account mapper. *//*

    protected static final AccountMapper ACCOUNT_MAPPER = Mappers.getMapper(AccountMapper.class);

    */
/** Candle mapper. *//*

    protected static final CandleMapper CANDLE_MAPPER = Mappers.getMapper(CandleMapper.class);

    */
/** Ticker mapper. *//*

    protected static final TickerMapper TICKER_MAPPER = Mappers.getMapper(TickerMapper.class);

    */
/** Order mapper. *//*

    protected static final OrderMapper ORDER_MAPPER = Mappers.getMapper(OrderMapper.class);

    */
/** Trade mapper. *//*

    protected static final TradeMapper TRADE_MAPPER = Mappers.getMapper(TradeMapper.class);

    */
/** Position mapper. *//*

    protected static final PositionMapper POSITION_MAPPER = Mappers.getMapper(PositionMapper.class);
*/



    protected static UtilMapper UTIL_MAPPER;
    protected static CurrencyMapper CURRENCY_MAPPER ;
    protected static StrategyMapper STRATEGY_MAPPER ;
    protected static AccountMapper ACCOUNT_MAPPER ;
    protected static CandleMapper CANDLE_MAPPER ;
    protected static TickerMapper TICKER_MAPPER;
    protected static OrderMapper ORDER_MAPPER ;
    protected static TradeMapper TRADE_MAPPER;
    protected static PositionMapper POSITION_MAPPER;


}
