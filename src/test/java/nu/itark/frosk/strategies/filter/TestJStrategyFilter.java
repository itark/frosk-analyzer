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
package nu.itark.frosk.strategies.filter;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.OpenFeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.analysis.TradeDTO;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


/**
 * 2-Period RSI Strategy
 * <p>
 * @see //stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategyFilter extends BaseIntegrationTest  {

    @Autowired
    StrategyFilter strategyFilter;

    @Autowired
    StrategiesMap strategiesMap;
	 
    @Test
    public final void testLongTradesOne(){
        final List<TradeDTO> longTrades = strategyFilter.getLongTradesAllStrategies(SimpleMovingMomentumStrategy.class.getSimpleName());
        System.out.println("longTrades.size():"+longTrades.size());
        longTrades.forEach(st-> {
            System.out.println("Trade:" + ReflectionToStringBuilder.toString(st));
        });
    }

    @Test
    public final void testShortTradesOne(){
        final List<TradeDTO> shortTrades = strategyFilter.getShortTrades(SimpleMovingMomentumStrategy.class.getSimpleName());
        System.out.println("shortTrades.size():"+shortTrades.size());
        shortTrades.forEach(st-> {
            System.out.println("Trade:" + ReflectionToStringBuilder.toString(st));
        });
    }

    @Test
    public final void testOpenTradesAll() {
        final List<TradeDTO> openTrades = strategyFilter.getLongTradesAllStrategies();
        System.out.println("openTrades:" + openTrades.size());
        openTrades.forEach(st-> {
            System.out.println("StrategyTrade:" + ReflectionToStringBuilder.toString(st));
        });
    }

    @Test
    public final void testOpenTradesAll2(){
        List<String> strategies = strategiesMap.buildStrategiesMap();
        strategies.forEach(s -> {
            final List<TradeDTO> openTrades = strategyFilter.getLongTradesAllStrategies(s);
            openTrades.forEach(System.out::println);
        });
    }

    @Test
    public void testSmartSignals() {
        final List<OpenFeaturedStrategyDTO> openSmartSignals = strategyFilter.getSmartSignals(false);
        System.out.println("openSmartSignals.size:"+openSmartSignals.size());
        openSmartSignals.forEach(signal -> {
            //System.out.println(signal.getLatestTrade() + ", " +signal.getSecurityName() + ", " +signal.getName() + ", "+signal.getTotalProfit() + ", "+signal.getProfitableTradesRatio() + ", "+signal.isOpen()+ ", "+signal.getNumberofTrades());
            System.out.println("signal:" + ReflectionToStringBuilder.toString(signal, ToStringStyle.NO_CLASS_NAME_STYLE));
        });
    }

}
