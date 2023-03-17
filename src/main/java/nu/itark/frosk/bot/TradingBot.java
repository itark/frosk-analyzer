/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2021 Ta4j Organization & respective
 * authors (see AUTHORS)
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
package nu.itark.frosk.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.num.DoubleNum;


@Service
@Slf4j
public class TradingBot {

    public void run(Strategy strategy, BarSeries barSeries) {
        int runBeginIndex = barSeries.getBeginIndex();
        int runEndIndex = barSeries.getEndIndex();
        log.trace("Running strategy (indexes: {} -> {}): {}", new Object[]{runBeginIndex, runEndIndex, strategy});
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        int seriesMaxSize;

        Rule entryRule = strategy.getEntryRule();
        Rule exitRule = strategy.getExitRule();

        for (seriesMaxSize = runBeginIndex; seriesMaxSize <= runEndIndex; ++seriesMaxSize) {

            if(entryRule.isSatisfied(seriesMaxSize)) {
              log.info("entryRule:{}",strategy.getEntryRule());

            }

            if (strategy.shouldEnter(seriesMaxSize)) {
                // Our strategy should enter
                boolean entered = tradingRecord.enter(seriesMaxSize, barSeries.getBar(seriesMaxSize).getClosePrice(), DoubleNum.valueOf(10));
                if (entered) {
                    Trade entry = tradingRecord.getLastEntry();
                    log.info("Entered on " + entry.getIndex() + " (price=" + entry.getNetPrice().doubleValue() + ", amount=" + entry.getAmount().doubleValue() + ")");

                    /*log.info("entryRule:{}",strategy.getEntryRule()();*/

                }
            } else if (strategy.shouldExit(seriesMaxSize)) {
                // Our strategy should exit
                boolean exited = tradingRecord.exit(seriesMaxSize, barSeries.getBar(seriesMaxSize).getClosePrice(), DoubleNum.valueOf(10));
                if (exited) {
                    Trade exit = tradingRecord.getLastExit();
                    log.info("Exited on " + exit.getIndex() + " (price=" + exit.getNetPrice().doubleValue() + ", amount=" + exit.getAmount().doubleValue() + ")");
                }
            }
        }
    }

}
