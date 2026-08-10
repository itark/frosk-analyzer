package nu.itark.frosk.analysis;

import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.GapPercentIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Diagnostic-only, not a permanent fixture: breaks down NewsBreakoutStrategy's
 * -0.84%/trade result (see StrategyComparisonReportIT#dailyReport) by exit
 * reason and entry trigger, to find out WHERE the losing edge comes from
 * rather than just that it loses.
 *
 * <p>Duplicates NewsBreakoutStrategy's indicator construction on purpose —
 * this reads the trading record after the fact and needs the same indicator
 * values at specific bars, not a hook into production code. Delete this file
 * once the strategy has been redesigned or shelved; it has no reason to
 * outlive the investigation.
 */
public class NewsBreakoutStrategyDiagnosticIT extends BaseIntegrationTest {

    private static final int    EMA_FAST       = 9;
    private static final int    EMA_SLOW       = 21;
    private static final int    ATR_PERIOD     = 14;
    private static final double ATR_TRAIL_MULT = 2.0;
    private static final int    MAX_BARS_HELD  = 12;
    private static final double GAP_UP_MIN_PCT = 1.5;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    StrategiesMap strategiesMap;

    @Autowired
    HedgeIndexService hedgeIndexService;

    @Test
    public void breakDownLossesByExitReasonAndEntryTrigger() {
        hedgeIndexService.warmCache();
        List<BarSeries> seriesList = barSeriesService.getDataSet(Database.YAHOO);
        assertFalse(seriesList.isEmpty(), "No YAHOO series in test database");

        Map<String, ExitBucket> byExitReason = new LinkedHashMap<>();
        Map<String, ExitBucket> byEntryTrigger = new LinkedHashMap<>();
        int[] pnlHistogram = new int[7]; // <-5, -5..-2, -2..-1, -1..0, 0..1, 1..5, >5
        int totalTrades = 0;
        int totalBarsHeld = 0;

        for (BarSeries series : seriesList) {
            if (series.getBarData().isEmpty()) continue;

            ClosePriceIndicator close   = new ClosePriceIndicator(series);
            EMAIndicator emaFast        = new EMAIndicator(close, EMA_FAST);
            EMAIndicator emaSlow        = new EMAIndicator(close, EMA_SLOW);
            ATRIndicator atr            = new ATRIndicator(series, ATR_PERIOD);
            GapPercentIndicator gapPct  = new GapPercentIndicator(series);

            Strategy strategy;
            TradingRecord record;
            try {
                strategy = strategiesMap.getStrategyToRun("NewsBreakoutStrategy", series);
                record = barSeriesService.runConfiguredStrategy(series, strategy);
            } catch (Exception e) {
                continue;
            }

            for (Position position : record.getPositions()) {
                if (!position.isClosed()) continue;
                int entryIdx = position.getEntry().getIndex();
                int exitIdx = position.getExit().getIndex();
                double pnlPct = (position.getGrossReturn().doubleValue() - 1) * 100;
                if (Double.isNaN(pnlPct)) continue;

                totalTrades++;
                totalBarsHeld += (exitIdx - entryIdx);

                // ── Classify exit reason — same left-to-right precedence as
                // atrTrail.or(emaCross).or(timeExit) in NewsBreakoutStrategy.
                Num highestSinceEntry = close.getValue(entryIdx);
                for (int i = entryIdx + 1; i <= exitIdx; i++) {
                    Num v = close.getValue(i);
                    if (v.isGreaterThan(highestSinceEntry)) highestSinceEntry = v;
                }
                Num stopLevel = highestSinceEntry.minus(atr.getValue(exitIdx).multipliedBy(series.numOf(ATR_TRAIL_MULT)));
                boolean atrTrailHit = close.getValue(exitIdx).isLessThanOrEqual(stopLevel);
                boolean emaCrossHit = emaFast.getValue(exitIdx).isLessThan(emaSlow.getValue(exitIdx));
                boolean timeExitHit = (exitIdx - entryIdx) >= MAX_BARS_HELD;

                String exitReason = atrTrailHit ? "atrTrailingStop"
                        : emaCrossHit ? "emaCrossDown"
                        : timeExitHit ? "maxBarsHeld"
                        : "other/inherentExit";
                byExitReason.computeIfAbsent(exitReason, k -> new ExitBucket()).add(pnlPct);

                // ── Classify entry trigger — gapUp.or(momentumMove) precedence.
                double gapAtEntry = gapPct.getValue(entryIdx).doubleValue();
                String entryTrigger = gapAtEntry > GAP_UP_MIN_PCT ? "gapUp" : "momentumMove";
                byEntryTrigger.computeIfAbsent(entryTrigger, k -> new ExitBucket()).add(pnlPct);

                // ── PnL histogram
                int bucket = pnlPct < -5 ? 0 : pnlPct < -2 ? 1 : pnlPct < -1 ? 2
                        : pnlPct < 0 ? 3 : pnlPct < 1 ? 4 : pnlPct < 5 ? 5 : 6;
                pnlHistogram[bucket]++;
            }
        }

        final int finalTotalTrades = totalTrades;
        System.out.println("\n=== NewsBreakoutStrategy diagnostic (" + totalTrades + " closed trades) ===");
        System.out.println("avg bars held: " + (totalTrades > 0 ? (double) totalBarsHeld / totalTrades : 0));

        System.out.println("\n-- by exit reason --");
        System.out.printf("%-20s %8s %10s %10s%n", "REASON", "n", "share%", "avgPnL%");
        byExitReason.forEach((reason, b) -> System.out.printf("%-20s %8d %10.1f %10.3f%n",
                reason, b.count, 100.0 * b.count / finalTotalTrades, b.avg()));

        System.out.println("\n-- by entry trigger --");
        System.out.printf("%-20s %8s %10s %10s%n", "TRIGGER", "n", "share%", "avgPnL%");
        byEntryTrigger.forEach((trigger, b) -> System.out.printf("%-20s %8d %10.1f %10.3f%n",
                trigger, b.count, 100.0 * b.count / finalTotalTrades, b.avg()));

        System.out.println("\n-- PnL histogram --");
        String[] labels = {"<-5%", "-5..-2%", "-2..-1%", "-1..0%", "0..1%", "1..5%", ">5%"};
        for (int i = 0; i < pnlHistogram.length; i++) {
            System.out.printf("%-10s %6d  (%.1f%%)%n", labels[i], pnlHistogram[i], 100.0 * pnlHistogram[i] / totalTrades);
        }
        System.out.println();
    }

    private static class ExitBucket {
        int count;
        double sumPnl;

        void add(double pnlPct) {
            count++;
            sumPnl += pnlPct;
        }

        double avg() {
            return count > 0 ? sumPnl / count : 0;
        }
    }
}
