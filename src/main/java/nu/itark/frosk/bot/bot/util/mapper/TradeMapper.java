package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.domain.Trade;
import nu.itark.frosk.bot.bot.dto.trade.TradeDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyAmountDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import org.knowm.xchange.dto.trade.UserTrade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * Trade mapper.
 */
@Mapper(uses = {UtilMapper.class, CurrencyMapper.class}, nullValuePropertyMappingStrategy = IGNORE)
public interface TradeMapper {

    // =================================================================================================================
    // XChange to DTO.


    @Mapping(source = "id", target = "tradeId")
    @Mapping(target = "uid", ignore = true)
    @Mapping(source = "source", target = "amount", qualifiedByName = "mapUserTradeToTradeDTOAmount")
    @Mapping(source = "source", target = "price", qualifiedByName = "mapUserTradeToTradeDTOPrice")
    @Mapping(source = "source", target = "fee", qualifiedByName = "mapUserTradeToTradeDTOFee")
    @Mapping(target = "order", ignore = true)
    @Mapping(source = "orderUserReference", target = "userReference")
    @Mapping(source = "instrument", target = "currencyPair")

    TradeDTO mapToTradeDTO(UserTrade source);

    @Named("mapUserTradeToTradeDTOAmount")

    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)

    default CurrencyAmountDTO mapUserTradeToTradeDTOAmount(UserTrade source) {
        CurrencyPairDTO cp = new CurrencyPairDTO(source.getInstrument());
        if (source.getOriginalAmount() != null && source.getInstrument() != null) {
            return CurrencyAmountDTO.builder()
                    .value(source.getOriginalAmount())
                    .currency(cp.getBaseCurrency())
                    .build();
        } else {
            return null;
        }
    }

    @Named("mapUserTradeToTradeDTOPrice")
    default CurrencyAmountDTO mapUserTradeToTradeDTOPrice(UserTrade source) {
        CurrencyPairDTO cp = new CurrencyPairDTO(source.getInstrument());
        if (source.getPrice() != null && source.getInstrument() != null) {
            return CurrencyAmountDTO.builder()
                    .value(source.getPrice())
                    .currency(cp.getQuoteCurrency())
                    .build();
        } else {
            return null;
        }
    }

    @Named("mapUserTradeToTradeDTOFee")
    default CurrencyAmountDTO mapUserTradeToTradeDTOFee(UserTrade source) {
        if (source.getFeeAmount() != null && source.getFeeCurrency() != null) {
            return CurrencyAmountDTO.builder()
                    .value(source.getFeeAmount())
                    .currency(new CurrencyDTO(source.getFeeCurrency().toString()))
                    .build();
        } else {
            return null;
        }
    }

    // =================================================================================================================
    // DTO to domain.


    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "order", ignore = true)

    Trade mapToTrade(TradeDTO source);


    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "order", ignore = true)

    void updateTrade(TradeDTO source, @MappingTarget Trade target);

    // =================================================================================================================
    // Domain to DTO.


    @Mapping(target = "order.trades", ignore = true)
    @Mapping(target = "orderId", source = "order.orderId")

    TradeDTO mapToTradeDTO(Trade source);

}
