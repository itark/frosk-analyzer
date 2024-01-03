package nu.itark.frosk.bot.bot.service;

import nu.itark.frosk.bot.bot.dto.user.AccountDTO;
import nu.itark.frosk.bot.bot.dto.user.UserDTO;

import java.util.Map;
import java.util.Optional;

/**
 * Service getting information about user, accounts and balances.
 */
public interface UserService {

    /**
     * Retrieve user information from exchange (user, accounts and balances).
     *
     * @return user from exchange
     */
    Optional<UserDTO> getUser();

    /**
     * Retrieve user accounts information from exchange (accounts and balances).
     *
     * @return accounts
     */
    Map<String, AccountDTO> getAccounts();

    /**
     * Retrieve user accounts information from cache - used by GraphQL API.
     *
     * @return accounts
     */
    Map<String, AccountDTO> getAccountsFromCache();

}
