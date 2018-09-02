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

import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.Bar;
import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;

import nu.itark.frosk.service.TimeSeriesService;


/**
 * 2-Period RSI Strategy
 * <p>
 * @see http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJMovingMomentumStrategy {

	Logger logger = Logger.getLogger(TestJMovingMomentumStrategy.class.getName());
	 
	 
	 @Autowired
	 TimeSeriesService timeSeriesService;
	 

    @Test
    public final void run() throws Exception {

		TimeSeries timeSeries = timeSeriesService.getDataSet("BOL.ST");      
		RSI2Strategy strat = new RSI2Strategy(timeSeries);

        Strategy strategy = strat.buildStrategy();
        TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        List<Trade> trades = tradingRecord.getTrades();    
     
        for (Trade trade : trades) {
        	Bar barEntry = timeSeries.getBar(trade.getEntry().getIndex());
        	logger.info(timeSeries.getName()+"::barEntry="+barEntry.getDateName());
        	Bar barExit = timeSeries.getBar(trade.getExit().getIndex());
        	logger.info(timeSeries.getName()+"::barExit="+barExit.getDateName());
            Decimal closePriceBuy = barEntry.getClosePrice();
            Decimal closePriceSell = barExit.getClosePrice();
            Decimal profit = closePriceSell.minus(closePriceBuy);
            
            if (trade.isOpened()) {
            	logger.info("isOpened():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            if (trade.isNew()) {
            	logger.info("isNew():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            if (trade.isClosed()) {
            	logger.info("isClose():barExit.getDateName()"+barExit.getDateName());
            } else {
            	logger.info("isNOTCLOSED():barEntry.getDateName()"+barEntry.getDateName());
            }
            
            logger.info("profit="+profit);
            
        }
        
        logger.info("Number of trades for the strategy: " + tradingRecord.getTradeCount());

        // Analysis
        logger.info("Total profit for the strategy: " + new TotalProfitCriterion().calculate(timeSeries, tradingRecord));
        double totalProfit = new TotalProfitCriterion().calculate(timeSeries, tradingRecord);
        double totalProfitPercentage = (totalProfit - 1 ) *100;  //TODO minus
        logger.info("Total profit for the strategy (%): "+ totalProfitPercentage);
    }

}
