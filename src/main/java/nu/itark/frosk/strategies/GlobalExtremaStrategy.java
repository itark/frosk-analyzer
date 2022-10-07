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

import nu.itark.frosk.model.StrategyIndicatorValue;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Strategies which compares current price to global extrema over a week.
 */
public class GlobalExtremaStrategy implements IIndicatorValue {

    // We assume that there were at least one trade every 5 minutes during the whole week
    private static final int NB_TICKS_PER_WEEK = 12 * 24 * 7;

	BarSeries series = null;
   
	public GlobalExtremaStrategy(BarSeries series) {
		this.series = series;
	}	
    
    /**
     * @return a global extrema strategy
     */
    public Strategy buildStrategy() {
        indicatorValues.clear();
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);

        // Getting the max price over the past week
        HighPriceIndicator maxPrices = new HighPriceIndicator(series);
        HighestValueIndicator weekMaxPrice = new HighestValueIndicator(maxPrices, NB_TICKS_PER_WEEK);
        // Getting the min price over the past week
        LowPriceIndicator minPrices = new LowPriceIndicator(series);
        LowestValueIndicator weekMinPrice = new LowestValueIndicator(minPrices, NB_TICKS_PER_WEEK);

        // Going long if the close price goes below the min price
        TransformIndicator downWeek = TransformIndicator.plus(weekMinPrice, 1.004);
        Rule buyingRule = new UnderIndicatorRule(closePrices, downWeek);

        // Going short if the close price goes above the max price
        TransformIndicator upWeek = TransformIndicator.plus(weekMaxPrice, 0.996);

        Rule sellingRule = new OverIndicatorRule(closePrices, upWeek);

        return new BaseStrategy("GlobalExtremaStrategy", buyingRule, sellingRule);
    }

    public Strategy buildStrategy(int lossPercentage) {
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);

        // Getting the max price over the past week
        HighPriceIndicator maxPrices = new HighPriceIndicator(series);
        HighestValueIndicator weekMaxPrice = new HighestValueIndicator(maxPrices, NB_TICKS_PER_WEEK);
        // Getting the min price over the past week
        LowPriceIndicator minPrices = new LowPriceIndicator(series);
        LowestValueIndicator weekMinPrice = new LowestValueIndicator(minPrices, NB_TICKS_PER_WEEK);

        // Going long if the close price goes below the min price
        TransformIndicator downWeek = TransformIndicator.plus(weekMinPrice, 1.004);
        Rule buyingRule = new UnderIndicatorRule(closePrices, downWeek);

        // Going short if the close price goes above the max price
        TransformIndicator upWeek = TransformIndicator.plus(weekMaxPrice, 0.996);

        Rule sellingRule = new OverIndicatorRule(closePrices, upWeek)
        		.or(new StopLossRule(  closePrices, series.numOf(lossPercentage)));

        return new BaseStrategy("GlobalExtremaStrategy", buyingRule, sellingRule);
    }

    public Strategy buildStrategy(int lossPercentage, int gainPercentage) {
        if (this.series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);

        // Getting the max price over the past week
        HighPriceIndicator maxPrices = new HighPriceIndicator(series);
        HighestValueIndicator weekMaxPrice = new HighestValueIndicator(maxPrices, NB_TICKS_PER_WEEK);
        // Getting the min price over the past week
        LowPriceIndicator minPrices = new LowPriceIndicator(series);
        LowestValueIndicator weekMinPrice = new LowestValueIndicator(minPrices, NB_TICKS_PER_WEEK);

        // Going long if the close price goes below the min price
        TransformIndicator downWeek = TransformIndicator.plus(weekMinPrice, 1.004);
        Rule buyingRule = new UnderIndicatorRule(closePrices, downWeek);

        // Going short if the close price goes above the max price
        TransformIndicator upWeek = TransformIndicator.plus(weekMaxPrice, 0.996);

        Rule sellingRule = new OverIndicatorRule(closePrices, upWeek)
        		.or(new StopLossRule( closePrices, series.numOf(lossPercentage)))
        		.or(new StopGainRule(closePrices, series.numOf(gainPercentage)));

        return new BaseStrategy("GlobalExtremaStrategy", buyingRule, sellingRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

}
