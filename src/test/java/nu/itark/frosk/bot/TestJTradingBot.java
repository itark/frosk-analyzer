package nu.itark.frosk.bot;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;

@SpringBootTest
public class TestJTradingBot   extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    TradingBot tradingBot;

    @Test
    public void testBot() {

        BarSeries barSeries = barSeriesService.getDataSet("BTC-USDT", false, false);
        SimpleMovingMomentumStrategy strat = new SimpleMovingMomentumStrategy(barSeries);
        Strategy strategy = strat.buildStrategy();
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);

        tradingBot.run(strategy, barSeries);
    }

}
