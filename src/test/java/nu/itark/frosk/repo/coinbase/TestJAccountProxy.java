package nu.itark.frosk.repo.coinbase;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.AccountProxy;
import nu.itark.frosk.crypto.coinbase.accounts.Account;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@Slf4j
public class TestJAccountProxy extends BaseIntegrationTest {


    @Autowired
    AccountProxy accountProxy;

   @Test
   public final void testGetAccounts() {
	   List<Account> accountList = accountProxy.getAccounts();
       print(accountList);
   }

    @Test
    public final void testGetAccountsWhenAvailable() {
        List<Account> accountFilteredList = accountProxy.getAccounts().stream()
                .filter(a-> a.getAvailable().compareTo(BigDecimal.ZERO) == 1)
                .collect(Collectors.toList());

        print(accountFilteredList);
    }


    void print(List<Account> list) {
        list.forEach(o -> {
            log.info(ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });
    }

}
