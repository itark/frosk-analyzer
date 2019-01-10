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
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import nu.itark.frosk.model.StrategyIndicatorValue;


/**
 * Moving momentum strategy.
 * <p>
 * @see http://stockcharts.com/help/doku.php?id=chart_school:trading_strategies:moving_momentum
 */
public class MovingMomentumStrategy implements IndicatorValue {
	Logger logger = Logger.getLogger(MovingMomentumStrategy.class.getName()); 

	TimeSeries series = null;
	MACDIndicator macd = null;
	EMAIndicator shortEma, longEma = null;
	
//	SortedSet<StrategyIndicatorValue> indicatorValues = new TreeSet<>(
//			Comparator.comparing(StrategyIndicatorValue::getDate));	

	List<StrategyIndicatorValue> indicatorValues = new ArrayList<>();		
	
	
	public MovingMomentumStrategy(TimeSeries series) {
		this.series = series;
	}	

	public Strategy buildStrategy() {
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // The bias is bullish when the shorter-moving average moves above the longer moving average.
        // The bias is bearish when the shorter-moving average moves below the longer moving average.
        shortEma = new EMAIndicator(closePrice, 9); 
        setIndicatorValues(shortEma, series, "shortEma");
        
        longEma = new EMAIndicator(closePrice, 26); 
        setIndicatorValues(longEma, series, "longEma");

        
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);  

        macd = new MACDIndicator(closePrice, 9, 26); 
        EMAIndicator emaMacd = new EMAIndicator(macd, 18); 
        
        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2
        
        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, 80)) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

  
        
        
        return new BaseStrategy("MovingMomentumStrategy", entryRule, exitRule);
    }

    
    private void setIndicatorValues(EMAIndicator indicator, TimeSeries series, String name) {
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
		return indicatorValues;
	}    
    
    
}
