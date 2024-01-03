package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.domain.ImportedTicker;
import nu.itark.frosk.bot.bot.dto.market.TickerDTO;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Ticker mapper.
 */
@Mapper(uses = {CurrencyMapper.class})
public interface TickerMapper {

    // =================================================================================================================
    // XChange to DTO.

    @Mapping(source = "instrument", target = "currencyPair")
    TickerDTO mapToTickerDTO(Ticker source);

    // =================================================================================================================
    // Domain to DTO.

    TickerDTO mapToTickerDTO(ImportedTicker source);

}
