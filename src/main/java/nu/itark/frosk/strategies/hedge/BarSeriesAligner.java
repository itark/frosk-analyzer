package nu.itark.frosk.strategies.hedge;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class BarSeriesAligner {

    /**
     * Aligns multiple BarSeries by timestamp and returns trimmed, aligned series
     * of the same size. The aligned series will contain only bars that are
     * present in all input series (intersection).
     *
     * @param inputSeriesList List of BarSeries to align
     * @return List of aligned and trimmed BarSeries in the same order
     */
    public static List<BarSeries> alignByTimestamp(List<BarSeries> inputSeriesList) {
        // Step 1: Map each series to its timestamp map
        List<Map<ZonedDateTime, Bar>> timestampedBarsList = inputSeriesList.stream()
                .map(series -> {
                    Map<ZonedDateTime, Bar> map = new HashMap<>();
                    for (int i = 0; i < series.getBarCount(); i++) {
                        map.put(series.getBar(i).getEndTime(), series.getBar(i));
                    }
                    return map;
                })
                .collect(Collectors.toList());

        // Step 2: Get common timestamps (intersection)
        Set<ZonedDateTime> commonTimestamps = new HashSet<>(timestampedBarsList.get(0).keySet());
        for (int i = 1; i < timestampedBarsList.size(); i++) {
            commonTimestamps.retainAll(timestampedBarsList.get(i).keySet());
        }

        // Step 3: Sort timestamps
        List<ZonedDateTime> sortedTimestamps = new ArrayList<>(commonTimestamps);
        sortedTimestamps.sort(Comparator.naturalOrder());

        // Step 4: Rebuild aligned BarSeries
        List<BarSeries> alignedSeriesList = new ArrayList<>();
        for (Map<ZonedDateTime, Bar> barMap : timestampedBarsList) {
            //BaseBarSeries newSeries = new BaseBarSeries();

            BarSeries newSeries = new BaseBarSeriesBuilder().withName("-name").withNumTypeOf(DoubleNum.class).build();


            for (ZonedDateTime timestamp : sortedTimestamps) {
                newSeries.addBar(barMap.get(timestamp));
            }
            alignedSeriesList.add(newSeries);
        }

        return alignedSeriesList;
    }

    /**
     * Aligns multiple series and trims them from the end to a fixed size.
     *
     * @param inputSeriesList List of BarSeries to align
     * @param maxLength       Max number of bars to keep (from the end)
     * @return Aligned and trimmed BarSeries
     */
    public static List<BarSeries> alignAndTruncate(List<BarSeries> inputSeriesList, int maxLength) {
        List<BarSeries> aligned = alignByTimestamp(inputSeriesList);

        // Find shortest available size
        int minLength = aligned.stream()
                .mapToInt(BarSeries::getBarCount)
                .min()
                .orElse(0);

        int truncateLength = Math.min(minLength, maxLength);

        // SubSeries from the end
        return aligned.stream()
                .map(series -> series.getSubSeries(series.getBarCount() - truncateLength, series.getBarCount()))
                .collect(Collectors.toList());
    }
}

