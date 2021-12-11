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
import java.util.Date;
import java.util.List;

import nu.itark.frosk.dataset.IndicatorValue;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.candles.ThreeBlackCrowsIndicator;
import org.ta4j.core.indicators.candles.ThreeWhiteSoldiersIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;

import nu.itark.frosk.model.StrategyIndicatorValue;

/**
 * Three black crows indicator.
 * </p>
 * @see <a href="http://www.investopedia.com/terms/t/three_black_crows.asp">
 *     http://www.investopedia.com/terms/t/three_black_crows.asp</a>
 */
public class ThreeBlackWhiteStrategy implements IIndicatorValue {

	TimeSeries series = null;
	
//	List<StrategyIndicatorValue> indicatorValues = new ArrayList<>();
	   
	public ThreeBlackWhiteStrategy(TimeSeries series) {
		this.series = series;
	}	
	
	/**
     * @return a CCI correction strategy
     */
    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
  
        setIndicatorValues(closePrice, series, "closePrice");     
        
        ThreeWhiteSoldiersIndicator  bullish = new ThreeWhiteSoldiersIndicator(series, 100,series.numOf(5));
        ThreeBlackCrowsIndicator  bearish = new ThreeBlackCrowsIndicator(series, 100, 5);
        
        Rule entryRule = new BooleanIndicatorRule(bullish); // Bull trend
        Rule exitRule = new BooleanIndicatorRule(bearish); // Bear trend
        
        Strategy strategy = new BaseStrategy("ThreeBlackWhiteStrategy", entryRule, exitRule);
        return strategy;
    }
    
    private void setIndicatorValues(ClosePriceIndicator indicator, TimeSeries series, String name) {
		IndicatorValue iv = null;
		for (int i = 0; i < indicator.getTimeSeries().getBarCount(); i++) {
			long date = indicator.getTimeSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			long value =  indicator.getTimeSeries().getBar(i).getClosePrice().longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
 	} 

	@Override
	public List<IndicatorValue> getIndicatorValues() {
		return indicatorValues;
	}	    

}
