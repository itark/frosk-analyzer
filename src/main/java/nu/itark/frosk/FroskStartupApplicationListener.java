package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.model.dto.AccountTypeDTO;
import nu.itark.frosk.repo.TradingAccountRepository;
import nu.itark.frosk.service.TradingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Component
public class FroskStartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    HighLander highLander;

    @Autowired
    TradingAccountService tradingAccountService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        tradingAccountService.initTradingAccount();
        //highLander.runClean();
       highLander.runInstall(Database.COINBASE);
       //highLander.runCleanInstall(Database.COINBASE);
    }


}
