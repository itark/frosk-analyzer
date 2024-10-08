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
import nu.itark.frosk.dataset.TestJYahooDataManager;
import nu.itark.frosk.service.BarSeriesService;
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
import java.util.logging.Logger;

@Slf4j
@SpringBootTest
public class TestJHaramiStrategy extends BaseIntegrationTest {

//Logger logger = Logger.getLogger(TestJYahooDataManager.class.getName());
	 
	 @Autowired
	 BarSeriesService barSeriesService;

     @Autowired
     HaramiStrategy haramiStrategy;


    @Test
    public final void run() throws Exception {
		BarSeries timeSeries = barSeriesService.getDataSet("SOL-EUR", false, false);
        Strategy strategy = haramiStrategy.buildStrategy(timeSeries);
        BarSeriesManager seriesManager = new BarSeriesManager(timeSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        List<Position> positions = tradingRecord.getPositions();
     
        for (Position position : positions) {
        	Bar barEntry = timeSeries.getBar(position.getEntry().getIndex());
        	//log.info(timeSeries.getName()+"::barEntry="+barEntry.getDateName());
        	Bar barExit = timeSeries.getBar(position.getExit().getIndex());
        	//log.info(timeSeries.getName()+"::barExit="+barExit.getDateName());
            Num closePriceBuy = barEntry.getClosePrice();
            Num closePriceSell = barExit.getClosePrice();
            Num profit = closePriceSell.minus(closePriceBuy);
            
           // logger.info("profit="+profit);

            log.info("Position: {}", ReflectionToStringBuilder.toString(position));
            
        }
        
        log.info("Number of positions for the strategy: " + tradingRecord.getPositionCount());

        // Analysis
        log.info("Profit: " + new ProfitCriterion().calculate(timeSeries, tradingRecord));
        log.info("Return: " + new ReturnCriterion().calculate(timeSeries, tradingRecord));
    }

}
