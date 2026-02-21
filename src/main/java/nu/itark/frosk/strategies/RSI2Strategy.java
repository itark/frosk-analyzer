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

import lombok.Data;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import nu.itark.frosk.model.StrategyIndicatorValue;


/**
 * 2-Period RSI Strategy
 * <p>
 * @see //stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */
@Component
public class RSI2Strategy extends AbstractStrategy implements IIndicatorValue {
	private RSIIndicator rsi = null;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
		indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 5);
		setIndicatorValues(shortEma, "shortEma");
        EMAIndicator longEma = new EMAIndicator(closePrice, 200);
        setIndicatorValues(longEma, "longEma");
        rsi = new RSIIndicator(closePrice, 2);
		setIndicatorValues(rsi, "rsi");
        // Entry rule
        // The long-term trend is up when a security is above its 200-period SMA.
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
               .and(new CrossedDownIndicatorRule(rsi, 10)) // Signal 1
                .and(new OverIndicatorRule(shortEma, closePrice)); // Signal 2

        Rule exitRule;
        if (!inherentExitRule) {
            // The long-term trend is down when a security is below its 200-period SMA.
            exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                    .and(new CrossedUpIndicatorRule(rsi, 80)) // Signal 1
                    .and(new UnderIndicatorRule(shortEma, closePrice)); // Signal 2
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
