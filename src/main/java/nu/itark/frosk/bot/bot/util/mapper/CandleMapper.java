package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.domain.ImportedCandle;
import nu.itark.frosk.bot.bot.dto.market.CandleDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
/**
 * Candle mapper.
 */
@Mapper(uses = {CurrencyMapper.class})
public interface CandleMapper {

    // =================================================================================================================
    // Domain to DTO.

    @Mapping(source = "currencyPairDTO", target = "currencyPair")
    CandleDTO mapToCandleDTO(ImportedCandle source);

}
