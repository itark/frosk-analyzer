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

import nu.itark.frosk.dataset.IndicatorValue;
import nu.itark.frosk.model.StrategyIndicatorValue;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * CCI Correction Strategy
 * <p>
 * @see ://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:cci_correction
 */
public class CCICorrectionStrategy implements IIndicatorValue {

	TimeSeries series = null;
	   
	public CCICorrectionStrategy(TimeSeries series) {
		this.series = series;
	}	
	
	/**
     * @return a CCI correction strategy
     */
    public Strategy buildStrategy() {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        CCIIndicator longCci = new CCIIndicator(series, 200);
        setIndicatorValues(longCci, series, "longCci");

        CCIIndicator shortCci = new CCIIndicator(series, 5);
        setIndicatorValues(shortCci, series, "shortCci");

        Num plus100 = series.numOf(100);
        Num minus100 = series.numOf(-100);
        
        Rule entryRule = new OverIndicatorRule(longCci, plus100) // Bull trend
                .and(new UnderIndicatorRule(shortCci, minus100)); // Signal
        
        Rule exitRule = new UnderIndicatorRule(longCci, minus100) // Bear trend
                .and(new OverIndicatorRule(shortCci, plus100)); // Signal
        
        Strategy strategy = new BaseStrategy("CCICorrectionStrategy", entryRule, exitRule);
        strategy.setUnstablePeriod(5);
        return strategy;
    }

    private void setIndicatorValues(CCIIndicator indicator, TimeSeries series, String name) {
        IndicatorValue iv = null;
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
