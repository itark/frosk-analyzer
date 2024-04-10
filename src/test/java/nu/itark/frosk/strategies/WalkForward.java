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

import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.service.BarSeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ProfitCriterion;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Walk-forward optimization example.
 * <p>
 * @see http://en.wikipedia.org/wiki/Walk_forward_optimization
 * @see http://www.futuresmag.com/2010/04/01/can-your-system-do-the-walk
 */
@SpringBootTest
public class WalkForward {
 
	Logger logger = Logger.getLogger(WalkForward.class.getName());
	
	 
	 @Autowired
	 BarSeriesService timeSeriesService;
	
	
    /**
     * Builds a list of split indexes from splitDuration.
     * @param series the time series to get split begin indexes of
     * @param splitDuration the duration between 2 splits
     * @return a list of begin indexes after split
     */
    public static List<Integer> getSplitBeginIndexes(BarSeries series, Duration splitDuration) {
        ArrayList<Integer> beginIndexes = new ArrayList<>();

        int beginIndex = series.getBeginIndex();
        int endIndex = series.getEndIndex();
        
        // Adding the first begin index
        beginIndexes.add(beginIndex);

        // Building the first interval before next split
        ZonedDateTime beginInterval = series.getFirstBar().getEndTime();
        ZonedDateTime endInterval = beginInterval.plus(splitDuration);

        for (int i = beginIndex; i <= endIndex; i++) {
            // For each tick...
            ZonedDateTime tickTime = series.getBar(i).getEndTime();
            if (tickTime.isBefore(beginInterval) || !tickTime.isBefore(endInterval)) {
                // Tick out of the interval
                if (!endInterval.isAfter(tickTime)) {
                    // Tick after the interval
                    // --> Adding a new begin index
                    beginIndexes.add(i);
                }

                // Building the new interval before next split
                beginInterval = endInterval.isBefore(tickTime) ? tickTime : endInterval;
                endInterval = beginInterval.plus(splitDuration);
            }
        }
        return beginIndexes;
    }
    
    /**
     * Returns a new time series which is a view of a subset of the current series.
     * <p>
     * The new series has begin and end indexes which correspond to the bounds of the sub-set into the full series.<br>
     * The tick of the series are shared between the original time series and the returned one (i.e. no copy).
     * @param series the time series to get a sub-series of
     * @param beginIndex the begin index (inclusive) of the time series
     * @param duration the duration of the time series
     * @return a constrained {@link BarSeries time series} which is a sub-set of the current series
     */
    public static BarSeries subseries(BarSeries series, int beginIndex, Duration duration) {

        // Calculating the sub-series interval
        ZonedDateTime beginInterval = series.getBar(beginIndex).getEndTime();
        ZonedDateTime endInterval = beginInterval.plus(duration);

        // Checking ticks belonging to the sub-series (starting at the provided index)
        int subseriesNbBars = 0;
        int endIndex = series.getEndIndex();
        for (int i = beginIndex; i <= endIndex; i++) {
            // For each tick...
            ZonedDateTime tickTime = series.getBar(i).getEndTime();
            if (tickTime.isBefore(beginInterval) || !tickTime.isBefore(endInterval)) {
                // Tick out of the interval
                break;
            }
            // Tick in the interval
            // --> Incrementing the number of ticks in the subseries
            subseriesNbBars++;
        }

        return series.getSubSeries(beginIndex, beginIndex + subseriesNbBars);
    }

    /**
     * Splits the time series into sub-series lasting sliceDuration.<br>
     * The current time series is splitted every splitDuration.<br>
     * The last sub-series may last less than sliceDuration.
     * @param series the time series to split
     * @param splitDuration the duration between 2 splits
     * @param sliceDuration the duration of each sub-series
     * @return a list of sub-series
     */
    public static List<BarSeries> splitSeries(BarSeries series, Duration splitDuration, Duration sliceDuration) {
        ArrayList<BarSeries> subseries = new ArrayList<>();
        if (splitDuration != null && !splitDuration.isZero()
                && sliceDuration != null && !sliceDuration.isZero()) {

            List<Integer> beginIndexes = getSplitBeginIndexes(series, splitDuration);
            for (Integer subseriesBegin : beginIndexes) {
                subseries.add(subseries(series, subseriesBegin, sliceDuration));
            }
        }
        return subseries;
    }

    @Test
    public final void run() throws Exception {
        // Splitting the series into slices
    	BarSeries timeSeries = timeSeriesService.getDataSet("BOL.ST", false, false);
        
//        List<TimeSeries> subseries = splitSeries(timeSeries, Duration.ofHours(6), Duration.ofDays(7));
        List<BarSeries> subseries = splitSeries(timeSeries, Duration.ofDays(12), Duration.ofDays(30));

        
        // Building the map of strategies
        Map<Strategy, String> strategies = StrategiesMap.buildStrategiesMap(timeSeries);

        // The analysis criterion
        AnalysisCriterion profitCriterion = new ProfitCriterion();

        for (BarSeries slice : subseries) {
            // For each sub-series...
//            System.out.println("Sub-series: " + slice.getSeriesPeriodDescription());
            BarSeriesManager sliceManager = new BarSeriesManager(slice);
            for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
                Strategy strategy = entry.getKey();
                String name = entry.getValue();
                // For each strategy...Œ
                TradingRecord tradingRecord = sliceManager.run(strategy);
                double profit = profitCriterion.calculate(slice, tradingRecord).doubleValue();
                System.out.println("\tProfit for " + name + ": " + profit);
            }
            Strategy bestStrategy = profitCriterion.chooseBest(sliceManager, new ArrayList<Strategy>(strategies.keySet()));
            System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");
        }
    }
    
}
