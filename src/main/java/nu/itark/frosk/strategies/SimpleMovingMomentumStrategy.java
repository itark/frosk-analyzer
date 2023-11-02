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

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.stereotype.Service;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ChandelierExitLongIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.*;

import java.util.List;

@Slf4j
public class SimpleMovingMomentumStrategy extends AbstractStrategy implements IIndicatorValue  {
    BarSeries series = null;
    EMAIndicator shortEma, longEma = null;

    public SimpleMovingMomentumStrategy(BarSeries series) {
        super(series);
        //this.series = super.barSeriesWithForecast;
        this.series = series;
    }

    public Strategy buildStrategy() {
        indicatorValues.clear();
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);
        shortEma = new EMAIndicator(openPrice, 10);
        longEma = new EMAIndicator(openPrice, 20);
        setIndicatorValues(shortEma, "shortEma");
        setIndicatorValues(longEma, "longEma");

        //System.out.println("series:"+series.getName()+" stationary="+stationary);


/*
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(series);
        IsRisingRule pSarIsRisingRule = new IsRisingRule(pSar, 1);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 1);
*/
/*
        Rule entryRuleX = new OverIndicatorRule(shortEma, longEma)
                .and(pSarIsRisingRule);
*/

        IsRisingRule openPriceIsRisingRule = new IsRisingRule(closePrice, 2);
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
                       // .and(openPriceIsRisingRule);

        Rule exitRule;
        ChandelierExitLongIndicator cel = new ChandelierExitLongIndicator(series, 5, 3);
        // ChandelierExitLongIndicator cel = new ChandelierExitLongIndicator(series);
        setIndicatorValues(cel, "cel");
        exitRule = new UnderIndicatorRule(openPrice,cel)
                .or(new StopLossRule(closePrice, 2))
                .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));

/*
        if (!inherentExitRule) {
            ChandelierExitLongIndicator cel = new ChandelierExitLongIndicator(series, 5, 3);
           // ChandelierExitLongIndicator cel = new ChandelierExitLongIndicator(series);
            setIndicatorValues(cel, "cel");
            exitRule = new UnderIndicatorRule(openPrice,cel)
                    .or(new StopLossRule(closePrice, 2))
                    .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));
        } else {
            exitRule = exitRule();
        }
*/

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }


}
