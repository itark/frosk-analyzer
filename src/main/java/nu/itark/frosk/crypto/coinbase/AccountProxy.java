package nu.itark.frosk.crypto.coinbase;

import nu.itark.frosk.crypto.coinbase.accounts.Account;
import nu.itark.frosk.crypto.coinbase.accounts.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
public class AccountProxy {

	@Autowired
    AccountService accountService;

    public List<Account> getAccounts() {
        return accountService.getAccounts();
    }



}
