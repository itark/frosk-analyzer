package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.hedge.HedgeIndexStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;

import java.util.List;

@Slf4j
@SpringBootTest
public class TestJHedgeIndexStrategy  extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    HedgeIndexStrategy hedgeIndexStrategy;

    @Test
    public final void run() throws Exception {
        BarSeries barSeries = barSeriesService.getDataSet("ABB.ST", false, false);
        Strategy strategy = hedgeIndexStrategy.buildStrategy(barSeries);
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        List<Position> positions = tradingRecord.getPositions();

        for (Position position : positions) {
            Bar barEntry = barSeries.getBar(position.getEntry().getIndex());
            //log.info(barSeries.getName()+"::barEntry="+barEntry.getDateName());
            Bar barExit = barSeries.getBar(position.getExit().getIndex());
            //log.info(barSeries.getName()+"::barExit="+barExit.getDateName());
            Num closePriceBuy = barEntry.getClosePrice();
            Num closePriceSell = barExit.getClosePrice();
            Num profit = closePriceSell.minus(closePriceBuy);

            // logger.info("profit="+profit);

            log.info("Position: {}", ReflectionToStringBuilder.toString(position));

        }

        log.info("Number of positions for the strategy: " + tradingRecord.getPositionCount());

        // Analysis
        log.info("Profit: " + new ProfitCriterion().calculate(barSeries, tradingRecord));
        log.info("Return: " + new ReturnCriterion().calculate(barSeries, tradingRecord));
    }


}
