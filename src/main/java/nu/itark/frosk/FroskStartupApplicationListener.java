package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.service.TradingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FroskStartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${frosk.highlander.cleaninstall}")
    private boolean cleanInstall;

    @Autowired
    HighLander highLander;

    @Autowired
    TradingAccountService tradingAccountService;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("cleanInstall:{}",cleanInstall);
        tradingAccountService.initTradingAccounts();
        if (cleanInstall) {
            highLander.runCleanInstall(Database.YAHOO);
        } else {
            highLander.runInstall(Database.YAHOO);
        }
        // Crypto's Tier-0 pipeline otherwise only fires on its 15-minute cron
        // (scheduler.crypto.intraday.cron); kick it once at startup so live/paper
        // trading isn't idle for up to 15 minutes after a restart. No-op on the
        // equity profile — syncCryptoIntraday() self-gates on frosk.run.crypto.
        highLander.syncCryptoIntraday();
    }

}
