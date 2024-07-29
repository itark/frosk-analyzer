/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;

import java.util.List;

@SpringBootTest
@Slf4j
public class TestJEngulfingStrategy extends BaseIntegrationTest {

	 @Autowired
	 BarSeriesService barSeriesService;

    @Autowired
    EngulfingStrategy engulfingStrategy;

    @Test
    public final void run() throws Exception {
		BarSeries barSeries = barSeriesService.getDataSet("ENS-EUR", false, false);
        Strategy strategy = engulfingStrategy.buildStrategy(barSeries);
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        List<Position> positions = tradingRecord.getPositions();
     
        for (Position trade : positions) {
        	Bar barEntry = barSeries.getBar(trade.getEntry().getIndex());
        	//log.info(barSeries.getName()+"::barEntry="+barEntry.getDateName());
        	Bar barExit = barSeries.getBar(trade.getExit().getIndex());
            //log.info(barSeries.getName()+"::barExit="+barExit.getDateName());
            Num closePriceBuy = barEntry.getClosePrice();
            Num closePriceSell = barExit.getClosePrice();
            Num profit = closePriceSell.minus(closePriceBuy);
            
           // log.info("profit="+profit);
            
        }
        
        log.info("Number of positions for the strategy: " + tradingRecord.getPositionCount());

        // Analysis
        log.info("Profit: " + new ProfitCriterion().calculate(barSeries, tradingRecord));
        log.info("Return: " + new ReturnCriterion().calculate(barSeries, tradingRecord));
        log.info("ProfitLossPercentage: " + new ProfitLossPercentageCriterion().calculate(barSeries, tradingRecord));


/*
        double totalProfitPercentage = (totalProfit - 1 ) *100;  //TODO minus
        logger.info("Total profit for the strategy (%): "+ totalProfitPercentage);
*/
    }

}
