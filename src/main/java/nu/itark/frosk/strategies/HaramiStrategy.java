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

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.candles.BearishHaramiIndicator;
import org.ta4j.core.indicators.candles.BullishHaramiIndicator;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;

/**
 * Bearish Harami pattern indicator.
 * </p>
 * @see <a href="http://www.investopedia.com/terms/b/bullishharami.asp">
 *     http://www.investopedia.com/terms/b/bullishharami.asp</a>
 */
public class HaramiStrategy {

	TimeSeries series = null;
	   
	public HaramiStrategy(TimeSeries series) {
		this.series = series;
	}	
	
	/**
     * @return a CCI correction strategy
     */
    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        BullishHaramiIndicator  bullish = new BullishHaramiIndicator(series);
        BearishHaramiIndicator  bearish = new BearishHaramiIndicator(series);
        
        Rule entryRule = new BooleanIndicatorRule(bullish); // Bull trend
        Rule exitRule = new BooleanIndicatorRule(bearish); // Bear trend
        
        Strategy strategy = new BaseStrategy("HaramiStrategy", entryRule, exitRule);
        return strategy;
    }

}
