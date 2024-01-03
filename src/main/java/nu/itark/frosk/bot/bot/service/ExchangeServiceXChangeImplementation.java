package nu.itark.frosk.bot.bot.service;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.util.base.service.BaseService;
import org.knowm.xchange.Exchange;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exchange service - XChange implementation of {@link ExchangeService}.
 */
@RequiredArgsConstructor
public class ExchangeServiceXChangeImplementation extends BaseService implements ExchangeService {

    /** XChange service. */
    private final Exchange exchange;

    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public Set<CurrencyPairDTO> getAvailableCurrencyPairs() {
        logger.debug("Retrieving available currency pairs");
/*
        return exchange.getExchangeMetaData()
                .getCurrencyPairs()
                .keySet()
                .stream()
                .peek(cp -> logger.debug(" - {} available", cp))
                .map(CURRENCY_MAPPER::mapToCurrencyPairDTO)
                .collect(Collectors.toCollection(LinkedHashSet::new));
*/
        return null;

    }

}
