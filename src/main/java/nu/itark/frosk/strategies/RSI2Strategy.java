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
import java.util.logging.Logger;

import nu.itark.frosk.dataset.IndicatorValue;
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
 * @see //stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */
public class RSI2Strategy implements IIndicatorValue {
	private RSIIndicator rsi = null;
	private TimeSeries series = null;

	public RSI2Strategy(TimeSeries series) {
		this.series = series;
	}

    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		setIndicatorValues(shortSma, "shortSma");
        SMAIndicator longSma = new SMAIndicator(closePrice, 200);
        setIndicatorValues(longSma, "longSma");

        // or selling opportunities within the bigger trend.
        rsi = new RSIIndicator(closePrice, 2);
		setIndicatorValues(rsi, "rsi");

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

        return new BaseStrategy("RSI2Strategy", entryRule, exitRule);
    }


    private void setIndicatorValues(SMAIndicator indicator, String name) {
		IndicatorValue iv;
		for (int i = 0; i < indicator.getTimeSeries().getBarCount(); i++) {
			long date = indicator.getTimeSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			long value =  indicator.getValue(i).longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
 	}

	private void setIndicatorValues(RSIIndicator indicator, String name) {
		IndicatorValue iv;
		for (int i = 0; i < indicator.getTimeSeries().getBarCount(); i++) {
			long date = indicator.getTimeSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			long value =  indicator.getValue(i).longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

    @Override
	public List<IndicatorValue> getIndicatorValues() {
		return indicatorValues;
	}

}
