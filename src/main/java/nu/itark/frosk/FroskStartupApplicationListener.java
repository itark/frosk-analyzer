package nu.itark.frosk;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.DataSetHelper;
import nu.itark.frosk.dataset.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FroskStartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    HighLander highLander;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //highLander.runClean();
       highLander.runInstall(Database.COINBASE);
       //highLander.runCleanInstall(Database.COINBASE);
    }
}
