package nu.itark.frosk.bot.bot.util.mapper;

import nu.itark.frosk.bot.bot.dto.user.AccountDTO;
import nu.itark.frosk.bot.bot.dto.user.BalanceDTO;
import nu.itark.frosk.bot.bot.dto.user.UserDTO;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Account mapper.
 */
@Mapper(uses = {CurrencyMapper.class})
public interface AccountMapper {

    // =================================================================================================================
    // XChange to DTO.


    @Mapping(source = "username", target = "id")
    @Mapping(source = "wallets", target = "accounts")

    UserDTO mapToUserDTO(AccountInfo source);

   @Mapping(source = "id", target = "accountId")
    @Mapping(target = "feature", ignore = true)
    @Mapping(target = "balances", source = "balances")
    @Mapping(target = "balance", ignore = true)
  AccountDTO mapToWalletDTO(Wallet source);

    default Set<BalanceDTO> mapToBalanceDTO(Map<Currency, Balance> source) {
        return source.values()
                .stream()
                .map(this::mapToBalanceDTO)
                .collect(Collectors.toSet());
    }

   @Mapping(source = "currency", target = "currency")
   BalanceDTO mapToBalanceDTO(Balance source);

}
