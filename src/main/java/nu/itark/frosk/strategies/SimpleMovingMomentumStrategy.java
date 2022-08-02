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

import nu.itark.frosk.dataset.IndicatorValue;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;

import java.util.List;


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

        ParabolicSarIndicator parabolicSarIndicator = new ParabolicSarIndicator(series);
        IsRisingRule isRisingRule = new IsRisingRule(parabolicSarIndicator, 1);
        IsFallingRule isFallingRule = new IsFallingRule(parabolicSarIndicator, 1);

        Rule entryRule = new OverIndicatorRule(shortEma, longEma) //Bullish trend
                .and(isRisingRule);

        Rule exitRule = isFallingRule
               .or(new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(2)));

        return new BaseStrategy("SimpleMovingMomentumStrategy", entryRule, exitRule);
    }


    @Override
    public List<IndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
