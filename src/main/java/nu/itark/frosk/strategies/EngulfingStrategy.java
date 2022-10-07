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
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR NIT‡
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.candles.BearishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

import java.util.List;

/**
* https://www.investopedia.com/terms/b/bearishengulfingp.asp
 */
public class EngulfingStrategy implements IIndicatorValue {

	BarSeries series = null;
	   
	public EngulfingStrategy(BarSeries series) {
		this.series = series;
	}	
	
	/**
     * @return a CCI correction strategy
     */
    public Strategy buildStrategy() {
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        BullishEngulfingIndicator  bullish = new BullishEngulfingIndicator(series);
        BearishEngulfingIndicator  bearish = new BearishEngulfingIndicator(series);
        
        Rule entryRule = new BooleanIndicatorRule(bullish); // Bull trend
        Rule exitRule = new BooleanIndicatorRule(bearish); // Bear trend

/*        Rule exitRule = new BooleanIndicatorRule(bearish)
                .or(new StopLossRule(closePrice, 2))
                .or(new StopGainRule(closePrice,2));*/

        setIndicatorValues(closePrice,"closePrice");

        Strategy strategy = new BaseStrategy("EngulfingStrategy", entryRule, exitRule);
        return strategy;
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
