/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nu.itark.frosk.strategies;

import lombok.SneakyThrows;
import nu.itark.frosk.dataset.IndicatorValue;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.util.List;
import java.util.logging.Logger;


public class SimpleMovingMomentumStrategy implements IIndicatorValue {
    TimeSeries series = null;
    EMAIndicator shortEma, longEma = null;

    public SimpleMovingMomentumStrategy(TimeSeries series) {
        this.series = series;
    }

    public Strategy buildStrategy() {
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        shortEma = new EMAIndicator(closePrice, 10);
        setIndicatorValues(shortEma, "shortEma");

        longEma = new EMAIndicator(closePrice, 20);
        setIndicatorValues(longEma, "longEma");

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma); // Trend
        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma); // Trend

/*
		Rule exitRule = new UnderIndicatorRule(shortEma, longEma)
				.or(new StopLossRule(closePrice, 2))
				.or(new StopGainRule(closePrice,2));
*/
        return new BaseStrategy("SimpleMovingMomentumStrategy", entryRule, exitRule);
    }

    private void setIndicatorValues(EMAIndicator indicator, String name) {
        IndicatorValue iv = null;
        for (int i = 0; i < indicator.getTimeSeries().getBarCount(); i++) {
            long date = indicator.getTimeSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
            long value = indicator.getValue(i).longValue();
            iv = new IndicatorValue(date, value, name);
            indicatorValues.add(iv);
        }
    }

    @Override
    public List<IndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
