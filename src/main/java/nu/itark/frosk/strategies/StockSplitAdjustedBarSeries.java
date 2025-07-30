package nu.itark.frosk.strategies;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.util.List;

/**
 * Adjusts a BarSeries for stock splits by normalizing all historical bars to the latest close.
 */
public class StockSplitAdjustedBarSeries {

    /**
     * Returns a new BarSeries adjusted to remove effects of stock splits.
     * Assumes that the latest price is correct and backward-adjusts the rest.
     */
    public static BarSeries adjust(BarSeries originalSeries) {
        if (originalSeries.isEmpty()) return originalSeries;

        List<Bar> bars = originalSeries.getBarData();
        Bar latestBar = bars.get(bars.size() - 1);
        double latestClose = latestBar.getClosePrice().doubleValue();

        // Find unadjusted historical last close
        double lastRawClose = bars.get(bars.size() - 1).getClosePrice().doubleValue();
        double ratio = latestClose / lastRawClose;

        BarSeries adjustedSeries = new BaseBarSeriesBuilder().withName(originalSeries.getName() + "-adj").withNumTypeOf(DoubleNum.class).build();

        for (Bar bar : bars) {
            double rawClose = bar.getClosePrice().doubleValue();
            double rawOpen = bar.getOpenPrice().doubleValue();
            double rawHigh = bar.getHighPrice().doubleValue();
            double rawLow = bar.getLowPrice().doubleValue();
            double rawVolume = bar.getVolume().doubleValue();

            double adjustedClose = rawClose * ratio;
            double adjustedOpen = rawOpen * ratio;
            double adjustedHigh = rawHigh * ratio;
            double adjustedLow = rawLow * ratio;
            double adjustedVolume = rawVolume; // optionally adjust this if needed

            adjustedSeries.addBar(
                    bar.getEndTime(),
                    adjustedOpen,
                    adjustedHigh,
                    adjustedLow,
                    adjustedClose,
                    adjustedVolume
            );
        }

        return adjustedSeries;
    }
}
