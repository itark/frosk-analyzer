/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Slf4j
public class TestJSimpleMovingMomentumStrategy extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    StrategiesMap strategiesMap;

    @Test
    public final void run() throws Exception {
        BarSeries timeSeries = barSeriesService.getDataSet("GRT-EUR", false, false);
        Strategy strategy = strategiesMap.getSimpleMovingMomentumStrategy().buildStrategy(timeSeries);
        BarSeriesManager seriesManager = new BarSeriesManager(timeSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);

        log.info("Total percentage:{} % ", new ProfitLossPercentageCriterion().calculate(timeSeries, tradingRecord).doubleValue());

        List<Position> positions = tradingRecord.getPositions();

        for (Position position : positions) {
            Bar barEntry = timeSeries.getBar(position.getEntry().getIndex());
            log.info("Entered on " + position.getEntry().getIndex() + " (close_price=" + barEntry.getClosePrice().doubleValue() + ", amount=" + barEntry.getAmount().doubleValue() + ")");
           // log.info(timeSeries.getName() + "::barEntry=" + barEntry.getDateName());
            Bar barExit = timeSeries.getBar(position.getExit().getIndex());
            log.info("Exited on " + position.getExit().getIndex() + " (close_price=" + barExit.getClosePrice().doubleValue() + ", amount=" + barExit.getAmount().doubleValue() + ")");
            //log.info(timeSeries.getName() + "::barExit=" + barExit.getDateName());
            log.info("position.profit=" + position.getProfit());
        }

        log.info("Number of positions for the strategy: " + tradingRecord.getPositionCount());

        // Analysis
/*
        AnalysisCriterion totalReturn = new ReturnCriterion();
        System.out.println("Total return: " + totalReturn.calculate(timeSeries, tradingRecord).doubleValue());
        totalPercentage = new ProfitLossPercentageCriterion();
        System.out.println("Total percentage: " + totalPercentage.calculate(timeSeries, tradingRecord).doubleValue());
*/


    }

}
