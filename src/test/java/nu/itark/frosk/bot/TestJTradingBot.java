package nu.itark.frosk.bot;

import net.minidev.json.JSONUtil;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.H2TestProfileJPAConfig;
import nu.itark.frosk.bot.bot.domain.Position;
import nu.itark.frosk.bot.bot.repository.OrderRepository;
import nu.itark.frosk.bot.bot.repository.PositionRepository;
import nu.itark.frosk.bot.bot.repository.StrategyRepository;
import nu.itark.frosk.bot.bot.repository.TradeRepository;
import nu.itark.frosk.bot.bot.service.PositionService;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;

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


    @Test
    public void helper() {

        strategyRepository.findAll().stream().forEach(o-> {
            System.out.println("o:"+ ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });

        positionRepository.findAll().stream().forEach(o-> {
            System.out.println("o:"+ ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });

        orderRepository.findAll().stream().forEach(o-> {
            System.out.println("o:"+ ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });

        positionService.getGains().forEach((c, g) -> {
            System.out.println(g);
        });


    }

    @Test
    public void clean() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        orderRepository.deleteAll();
        strategyRepository.deleteAll();

    }

    @Test
    public void testBot() {
        clean();

        BarSeries series = barSeriesService.getDataSet("GRT-EUR", false, false);
        SimpleMovingMomentumStrategy strat = new SimpleMovingMomentumStrategy(series);
        Strategy strategy = strat.buildStrategy();
        tradingBot.run(strategy, series);


/*
        //Hard clean
        positionRepository.deleteAll();
        orderRepository.deleteAll();
        strategyRepository.deleteAll();
*/

    }

/*
    @Test
    public void testBotWithCassandre() {
        BarSeries barSeries = barSeriesService.getDataSet("GRT-EUR", false, false);
        BTCStrategy strat = new BTCStrategy();
        Strategy strategy = strat.buildStrategy();
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        tradingBot.run(strategy, barSeries);
    }
*/

}
