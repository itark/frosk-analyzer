package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.dto.util.CurrencyAmountDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.util.jpa.CurrencyAmount;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Currency mapper.
 */
@Mapper
public interface CurrencyMapper {

    // =================================================================================================================
    // XChange to DTO.

    default String mapToCurrencyString(CurrencyDTO source) {
        if (source != null) {
            return source.toString();
        } else {
            return null;
        }
    }

    default CurrencyDTO mapToCurrencyDTO(String value) {
        return new CurrencyDTO(value);
    }

    @Mapping(source = "currencyCode", target = "code")
    CurrencyDTO mapToCurrencyDTO(Currency source);

    default String mapToCurrencyPairString(CurrencyPairDTO source) {
        return source.toString();
    }

    default CurrencyPairDTO mapToCurrencyPairDTO(Instrument source) {
        final CurrencyPair cp = (CurrencyPair) source;
        CurrencyDTO base = new CurrencyDTO(cp.base.getCurrencyCode());
        CurrencyDTO quote = new CurrencyDTO(cp.counter.getCurrencyCode());
        return new CurrencyPairDTO(base, quote);
    }

    default CurrencyPairDTO mapToCurrencyPairDTO(String source) {
        return new CurrencyPairDTO(source);
    }

    @Mapping(source = "base", target = "baseCurrency")
    @Mapping(source = "counter", target = "quoteCurrency")
    @Mapping(target = "baseCurrencyPrecision", ignore = true)
    @Mapping(target = "quoteCurrencyPrecision", ignore = true)

    CurrencyPairDTO mapToCurrencyPairDTO(CurrencyPair source);


    @Mapping(source = "value", target = "value")
    @Mapping(source = "currency", target = "currency")

    CurrencyAmountDTO mapToCurrencyAmountDTO(CurrencyAmount source);

    // =================================================================================================================
    // XChange to DTO.

    default Currency mapToCurrency(CurrencyDTO source) {
        if (source != null) {
            return new Currency(source.getCode());
        } else {
            return null;
        }
    }

    default CurrencyPair mapToCurrencyPair(CurrencyPairDTO source) {
        return new CurrencyPair(source.getBaseCurrency().getCode(), source.getQuoteCurrency().getCode());
    }

    default Instrument mapToInstrument(CurrencyPairDTO source) {
        return mapToCurrencyPair(source);
    }

    default Instrument mapToInstrument(CurrencyPair source) {
        return mapToCurrencyPair(mapToCurrencyPairDTO(source));
    }

}
