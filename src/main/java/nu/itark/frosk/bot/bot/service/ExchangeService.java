package nu.itark.frosk.bot.bot.service;

import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;

import java.util.Set;

/**
 * Service getting information about the exchange features.
 */
public interface ExchangeService {

    /**
     * Get the list of available currency pairs for trading.
     *
     * @return list of currency pairs
     */
    Set<CurrencyPairDTO> getAvailableCurrencyPairs();

}
