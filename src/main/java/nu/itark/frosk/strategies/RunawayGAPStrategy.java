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
import nu.itark.frosk.strategies.indicators.RunawayGAPIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.TrailingStopLossRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * https://www.investopedia.com/terms/r/runawaygap.asp
 */
@Component
public class RunawayGAPStrategy extends AbstractStrategy implements IIndicatorValue {

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
		indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RunawayGAPIndicator runawayGAPIndicator = new RunawayGAPIndicator(series);
        Rule entryRule = new BooleanIndicatorRule(runawayGAPIndicator);

        Rule exitRule;
        Num lossPercentage = DoubleNum.valueOf(2);
        if (!inherentExitRule) {
            exitRule = new TrailingStopLossRule(closePrice, lossPercentage, 3);
        } else {
            exitRule = exitRule();
        }


        Strategy strategy = new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
        return strategy;
    }

	@Override
	public List<StrategyIndicatorValue> getIndicatorValues() {
		return indicatorValues;
	}

}
