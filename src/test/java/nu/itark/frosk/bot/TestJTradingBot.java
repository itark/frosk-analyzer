package nu.itark.frosk.bot;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.H2TestProfileJPAConfig;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.EngulfingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {FroskApplication.class, H2TestProfileJPAConfig.class})
public class TestJTradingBot extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    TradingBot tradingBot;



    @Autowired
    TradingAccountService tradingAccountService;

    @Autowired
    EngulfingStrategy engulfingStrategy;


    @BeforeEach
    public void init() {
        tradingAccountService.initTradingAccounts();
    }

    @Test
    public void testBotRunningPosition() {
        //to zero
/*
        TradingAccount tradingAccount = tradingAccountRepository.findByAccountType(AccountTypeDTO.SANDBOX);
        tradingAccount.setAccountValue(BigDecimal.ZERO);
        tradingAccount.setSecurityValue(BigDecimal.ZERO);
        tradingAccountRepository.saveAndFlush(tradingAccount);
*/

        //TODO kör tradingBot.runningPositions och visa resultat i Card till vänster
        //0. Find first 200 bars from database
        //1.run StrategyAnalysis on 200 first bars
        //2. do securityMetaDataManager.getTopFeaturedStrategies()
        //3. step-wise barSeries and check when to enter and when to exist and keep calculation
        //3.0 find trailing 100 bars
        //3.1    BarSeriesUtils.addBars(barSeries, bars);
        //3.2 run below stuff

/*
        final List<FeaturedStrategyDTO> topFeaturedStrategies = strategyFilter.getTopFeaturedStrategies();
        topFeaturedStrategies.stream().forEach(dto-> {
            BarSeries series = barSeriesService.getDataSet( dto.getSecurityName(), false, false);
            Strategy strategyToRun = strategiesMap.getStrategyToRun(dto.getName()+"Strategy", series);
            tradingBot.runningPositions(strategyToRun, series);
        });
*/

        tradingBot.runningPositions();

        //System.out.println("tr:"+tradingAccountService.getTradingAccounts());

    }

}
