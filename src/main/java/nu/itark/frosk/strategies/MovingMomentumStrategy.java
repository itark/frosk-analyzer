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

import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.*;

import nu.itark.frosk.model.StrategyIndicatorValue;


/**
 * Moving momentum strategy.
 * <p>
 * @see //stockcharts.com/help/doku.php?id=chart_school:trading_strategies:moving_momentum
 */
@Component
public class MovingMomentumStrategy extends AbstractStrategy implements IIndicatorValue {
	MACDIndicator macd = null;
	EMAIndicator shortEma, longEma = null;

	public Strategy buildStrategy(BarSeries series) {
		super.setInherentExitRule();
		indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
		super.barSeries = series;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // The bias is bullish when the shorter-moving average moves above the longer moving average.
        // The bias is bearish when the shorter-moving average moves below the longer moving average.
        shortEma = new EMAIndicator(closePrice, 9); 
        setIndicatorValues(shortEma, "shortEma");
        longEma = new EMAIndicator(closePrice, 26);
        setIndicatorValues(longEma, "longEma");

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);
		setIndicatorValues(stochasticOscillK, "stochasticOscillK");
        macd = new MACDIndicator(closePrice, 9, 26);
		setIndicatorValues(macd, "macd");
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);
		setIndicatorValues(emaMacd, "emaMacd");
        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

		Rule exitRule;
		if (!inherentExitRule) {
			exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
					.and(new CrossedUpIndicatorRule(stochasticOscillK, 80)) // Signal 1
					.and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2
		} else {
			exitRule = exitRule();
		}

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

	@Override
	public List<StrategyIndicatorValue> getIndicatorValues() {
		return indicatorValues;
	}
    
}
