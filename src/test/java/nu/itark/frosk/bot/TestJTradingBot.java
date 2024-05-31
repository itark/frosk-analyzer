package nu.itark.frosk.bot;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.H2TestProfileJPAConfig;
import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.SecurityMetaDataManager;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.bot.bot.repository.OrderRepository;
import nu.itark.frosk.bot.bot.repository.PositionRepository;
import nu.itark.frosk.bot.bot.repository.StrategyRepository;
import nu.itark.frosk.bot.bot.repository.TradeRepository;
import nu.itark.frosk.bot.bot.service.PositionService;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.repo.TradingAccountRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.EngulfingStrategy;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.util.List;

import static nu.itark.frosk.bot.bot.dto.position.PositionStatusDTO.CLOSED;

@SpringBootTest(classes = {FroskApplication.class, H2TestProfileJPAConfig.class})
public class TestJTradingBot extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    TradingBot tradingBot;

    @Autowired
    PositionRepository positionRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    StrategyRepository strategyRepository;

    @Autowired
    PositionService positionService;

    @Autowired
    TradingAccountService tradingAccountService;

    @Autowired
    EngulfingStrategy engulfingStrategy;


    @BeforeEach
    public void init() {
        tradingAccountService.initTradingAccounts();
    }

    @Test
    public void helper() {

/*
        strategyRepository.findAll().stream().forEach(o -> {
            System.out.println("o:" + ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });

        positionRepository.findAll().stream().forEach(o -> {
            System.out.println("o:" + ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });

        orderRepository.findAll().stream().forEach(o -> {
            System.out.println("o:" + ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });
*/

        positionService.getGains().forEach((c, g) -> {

            final List<nu.itark.frosk.bot.bot.domain.Position> byStatus = positionRepository.findByStatus(CLOSED);

            final List<nu.itark.frosk.bot.bot.domain.Position> byAll = positionRepository.findAll();


            System.out.println(g);




        });


    }

    @Test
    public void clean() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        orderRepository.deleteAll();
        strategyRepository.deleteAll();
       // tradingAccountRepository.deleteAll();
    }

/*
    @Test
    public void testBotRun() {
      //  clean();

        final List<FeaturedStrategyDTO> topFeaturedStrategies = strategyFilter.getTopFeaturedStrategies();
        final String name = topFeaturedStrategies.get(0).getName();
        final String securityName = topFeaturedStrategies.get(0).getSecurityName();
        System.out.println("name:"+name+",securityName:"+securityName);
        Strategy strategy = null;
        BarSeries series = barSeriesService.getDataSet(securityName, false, false);
        if (name.equals("Engulfing")) {
            strategy  = engulfingStrategy.buildStrategy(series);
        }

       tradingBot.run(strategy, series);

    }
*/


/*
    @Test
    public void testBotRunning() {
        clean();

        final List<FeaturedStrategyDTO> topFeaturedStrategies = strategyFilter.getTopFeaturedStrategies();
        final String name = topFeaturedStrategies.get(0).getName();
        final String securityName = topFeaturedStrategies.get(0).getSecurityName();
        System.out.println("name:"+name+",securityName:"+securityName);
        Strategy strategy = null;
        BarSeries series = barSeriesService.getDataSet(securityName, false, false);

         topFeaturedStrategies.stream()
                .filter(fsDto -> fsDto.getName().equals("Engulfing"))
                 .forEach(eng -> {
                     Strategy strat  = engulfingStrategy.buildStrategy(series);
                    // tradingBot.running(strat, series);
                 });

*/
/*
        if (name.equals("Engulfing")) {
            strategy  = engulfingStrategy.buildStrategy(series);
        }
*//*

        tradingBot.running(engulfingStrategy.buildStrategy(series), series);
    }

*/
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
