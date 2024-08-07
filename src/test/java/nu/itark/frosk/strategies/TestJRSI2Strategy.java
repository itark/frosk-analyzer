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

import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.TestJYahooDataManager;
import nu.itark.frosk.service.BarSeriesService;
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


/**
 * 2-Period RSI Strategy
 * <p>
 * @see http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */

@SpringBootTest
public class TestJRSI2Strategy extends BaseIntegrationTest {

	Logger logger = Logger.getLogger(TestJYahooDataManager.class.getName());
	 
	 
	 @Autowired
	 BarSeriesService barSeriesService;

     @Autowired
    StrategiesMap strategiesMap;
	 

    @Test
    public final void run() throws Exception {
		BarSeries timeSeries = barSeriesService.getDataSet("BTC-EUR", false, false);
        Strategy strategy = strategiesMap.getRsiStrategy().buildStrategy(timeSeries);
        BarSeriesManager seriesManager = new BarSeriesManager(timeSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        List<Position> positions = tradingRecord.getPositions();
     
        for (Position position : positions) {
        	Bar barEntry = timeSeries.getBar(position.getEntry().getIndex());
        	logger.info(timeSeries.getName()+"::barEntry="+barEntry.getDateName());
        	Bar barExit = timeSeries.getBar(position.getExit().getIndex());
        	logger.info(timeSeries.getName()+"::barExit="+barExit.getDateName());
            Num closePriceBuy = barEntry.getClosePrice();
            Num closePriceSell = barExit.getClosePrice();
            Num profit = closePriceSell.minus(closePriceBuy);
            
            if (position.isOpened()) {
            	logger.info("isOpened():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            if (position.isNew()) {
            	logger.info("isNew():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            if (position.isClosed()) {
            	logger.info("isClose():barExit.getDateName()"+barExit.getDateName());
            } else {
            	logger.info("isNOTCLOSED():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            logger.info("profit="+profit);
            
        }
        
        logger.info("Number of positions for the strategy: " + tradingRecord.getPositionCount());

        // Analysis
        logger.info("Total profit for the strategy: " + new ProfitCriterion().calculate(timeSeries, tradingRecord));
        double totalProfit = new ReturnCriterion().calculate(timeSeries, tradingRecord).doubleValue();
        double totalProfitPercentage = (totalProfit - 1 ) *100;  //TODO minus
        logger.info("Total profit for the strategy (%): "+ totalProfitPercentage);
    }

}
