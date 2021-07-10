package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.accounts.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountProxy {

	@Autowired
    AccountService accountService;

    public List<Account> getAccounts() {
        return accountService.getAccounts();
    }



}
