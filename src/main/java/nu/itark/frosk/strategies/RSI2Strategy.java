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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import nu.itark.frosk.model.StrategyIndicatorValue;


/**
 * 2-Period RSI Strategy
 * <p>
 * @see http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */
public class RSI2Strategy implements IndicatorValue{

	Logger logger = Logger.getLogger(RSI2Strategy.class.getName());

	RSIIndicator rsi = null;
	TimeSeries series = null;
	
//	SortedSet<StrategyIndicatorValue> indicatorValues = new TreeSet<>(
//			Comparator.comparing(StrategyIndicatorValue::getDate));	
	
	List<StrategyIndicatorValue> indicatorValues = new ArrayList<>();		
	
	public RSI2Strategy(TimeSeries series) {
		this.series = series;
	}
	
	/**
     * @param series a time series
     * @return a 2-period RSI strategy
     */
    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator longSma = new SMAIndicator(closePrice, 200);
        
        setIndicatorValues(longSma, series, "longSma");


        // We use a 2-period RSI indicator to identify buying
        // or selling opportunities within the bigger trend.
        rsi = new RSIIndicator(closePrice, 2);
        
        // Entry rule
        // The long-term trend is up when a security is above its 200-period SMA.
        Rule entryRule = new OverIndicatorRule(shortSma, longSma) // Trend
                .and(new CrossedDownIndicatorRule(rsi, 5)) // Signal 1
                .and(new OverIndicatorRule(shortSma, closePrice)); // Signal 2
        
        // Exit rule
        // The long-term trend is down when a security is below its 200-period SMA.
        Rule exitRule = new UnderIndicatorRule(shortSma, longSma) // Trend
                .and(new CrossedUpIndicatorRule(rsi, 95)) // Signal 1
                .and(new UnderIndicatorRule(shortSma, closePrice)); // Signal 2
        
//        setIndicatorValues(rsi, series);
        
        return new BaseStrategy("RSI2Strategy", entryRule, exitRule);
    }

    
    private void setIndicatorValues(SMAIndicator indicator, TimeSeries series, String name) {
//    	logger.info("setIndicatorValues, name="+name);
    	StrategyIndicatorValue iv = null;
 		for (int i = 0; i < series.getBarCount(); i++) {
 			Date iDate = Date.from(series.getBar(i).getEndTime().toInstant());
// 			BigDecimal iBig = BigDecimal.valueOf(series.getBar(i).getMinPrice().doubleValue());
// 			logger.info(BigDecimal.valueOf(series.getBar(i).getMinPrice().doubleValue())+":series");
// 			logger.info(BigDecimal.valueOf(indicator.getValue(i).doubleValue())+":indicator");
 			BigDecimal iBig = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
// 			String indicatorStr = indicator.getClass().getSimpleName();
 			iv = new StrategyIndicatorValue(iDate, iBig, name);
 			indicatorValues.add(iv);
 		}
 		
 	}     
    
    
    
    
    
    @Override
	public SortedSet<StrategyIndicatorValue> getIndicatorValues() {
    	throw new RuntimeException("not use...");
	}

	@Override
	public List<StrategyIndicatorValue> getIndicatorValues2() {
		// TODO Auto-generated method stub
		return indicatorValues;
	}	    
    
}
