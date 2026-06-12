package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.service.IntradayDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.criteria.SqnCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Before/after PnL comparison harness for strategy redesigns.
 *
 * <p>Runs each strategy across the full dataset through the production
 * execution path ({@link BarSeriesService#runConfiguredStrategy}) — identical
 * fees and trade execution models — and prints aggregate metrics per strategy.
 * Nothing is written to the database, so this can be run repeatedly to compare
 * a baseline against a redesigned strategy.
 *
 * <p>Usage:
 * <pre>
 *   mvn test -Dtest=StrategyComparisonReportIT#dailyReport
 *   mvn test -Dtest=StrategyComparisonReportIT#intradayReport
 *   mvn test -Dtest=StrategyComparisonReportIT#dailyReport -Dfrosk.compare.strategies=HighLanderStrategy
 * </pre>
 */
public class StrategyComparisonReportIT extends BaseIntegrationTest {

    private static final List<String> DEFAULT_DAILY_STRATEGIES = List.of(
            "ShortTermMomentumLongTermStrengthStrategy",
            "HighLanderStrategy",
            "SwedishLongTermMomentumStrategy",
            "DailyOversoldBounceStrategy",
            "CANSLIMStrategy",
            "DailyBreakoutStrategy"
    );

    private static final List<String> INTRADAY_STRATEGIES = List.of(
            "OpeningRangeBreakoutIntradayStrategy",
            "VWAPMeanReversionIntradayStrategy",
            "GapReversalIntradayStrategy"
    );

    /** Round-trip fee drag (2 trades) in percent, for the intraday net column. */
    private static final double INTRADAY_ROUND_TRIP_FEE_PCT = 0.06;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    StrategiesMap strategiesMap;

    @Autowired
    HedgeIndexService hedgeIndexService;

    @Autowired
    IntradayDataService intradayDataService;

    @Test
    public void dailyReport() {
        hedgeIndexService.warmCache();
        List<BarSeries> seriesList = barSeriesService.getDataSet(Database.YAHOO);
        assertFalse(seriesList.isEmpty(), "No YAHOO series in test database");
        List<String> strategies = configuredStrategies(DEFAULT_DAILY_STRATEGIES);
        System.out.println("\n=== DAILY STRATEGY REPORT (" + seriesList.size() + " securities) ===");
        printReport(runAll(strategies, seriesList), false);
    }

    @Test
    public void intradayReport() {
        hedgeIndexService.warmCache();
        Map<Security, BarSeries> seriesMap = intradayDataService.buildAllSeriesFromDb();
        assertFalse(seriesMap.isEmpty(), "No intraday bars in test database — seed intraday_bar from production first");
        List<BarSeries> seriesList = new ArrayList<>(seriesMap.values());
        List<String> strategies = configuredStrategies(INTRADAY_STRATEGIES);
        System.out.println("\n=== INTRADAY STRATEGY REPORT (" + seriesList.size() + " securities, 15m bars) ===");
        printReport(runAll(strategies, seriesList), true);
    }

    private List<String> configuredStrategies(List<String> defaults) {
        String override = System.getProperty("frosk.compare.strategies");
        if (override != null && !override.isBlank()) {
            return Arrays.stream(override.split(",")).map(String::trim).toList();
        }
        return defaults;
    }

    private Map<String, Aggregate> runAll(List<String> strategies, List<BarSeries> seriesList) {
        Map<String, Aggregate> results = new LinkedHashMap<>();
        for (String strategyName : strategies) {
            Aggregate agg = new Aggregate();
            for (BarSeries series : seriesList) {
                if (series.getBarData().isEmpty()) {
                    continue;
                }
                try {
                    Strategy strategy = strategiesMap.getStrategyToRun(strategyName, series);
                    TradingRecord record = barSeriesService.runConfiguredStrategy(series, strategy);
                    agg.add(series, record);
                } catch (Exception e) {
                    agg.errors++;
                }
            }
            results.put(strategyName, agg);
        }
        return results;
    }

    private void printReport(Map<String, Aggregate> results, boolean intraday) {
        String header = String.format("%-45s %6s %8s %10s %8s %8s %10s %10s %8s %7s",
                "STRATEGY", "nSec", "nTrades", "avgProfit%", "avgSQN", "winRate%", "avgPnL/tr", intraday ? "netPnL/tr" : "expcy/tr", "avgMaxDD", "errors");
        System.out.println(header);
        System.out.println("-".repeat(header.length()));
        results.forEach((name, agg) -> {
            double avgPnlPerTrade = agg.closedTrades > 0 ? agg.sumPnlPerTrade / agg.closedTrades : 0;
            double secondPerTrade = intraday
                    ? avgPnlPerTrade - INTRADAY_ROUND_TRIP_FEE_PCT
                    : avgPnlPerTrade; // expectancy column reuses avg for daily; SQN carries the quality signal
            System.out.println(String.format("%-45s %6d %8d %10.2f %8.2f %8.1f %10.3f %10.3f %8.2f %7d",
                    name,
                    agg.nSeries,
                    agg.closedTrades,
                    agg.avg(agg.sumTotalProfit, agg.nProfitSamples),
                    agg.avg(agg.sumSqn, agg.nSqnSamples),
                    agg.closedTrades > 0 ? 100.0 * agg.winningTrades / agg.closedTrades : 0,
                    avgPnlPerTrade,
                    secondPerTrade,
                    agg.avg(agg.sumMaxDD, agg.nDDSamples) * 100,
                    agg.errors));
        });
        System.out.println();
    }

    private static class Aggregate {
        int nSeries;
        int closedTrades;
        int winningTrades;
        int errors;
        double sumTotalProfit;
        int nProfitSamples;
        double sumSqn;
        int nSqnSamples;
        double sumMaxDD;
        int nDDSamples;
        double sumPnlPerTrade;

        void add(BarSeries series, TradingRecord record) {
            nSeries++;
            for (Position position : record.getPositions()) {
                closedTrades++;
                double grossReturn = position.getGrossReturn().doubleValue();
                if (!Double.isNaN(grossReturn)) {
                    double pnlPct = (grossReturn - 1) * 100;
                    sumPnlPerTrade += pnlPct;
                    if (position.hasProfit()) {
                        winningTrades++;
                    }
                }
            }
            double totalProfit = new ProfitLossPercentageCriterion().calculate(series, record).doubleValue();
            if (!Double.isNaN(totalProfit)) {
                sumTotalProfit += totalProfit;
                nProfitSamples++;
            }
            if (record.getPositionCount() > 0) {
                double sqn = new SqnCriterion().calculate(series, record).doubleValue();
                if (!Double.isNaN(sqn)) {
                    sumSqn += sqn;
                    nSqnSamples++;
                }
                double maxDD = new MaximumDrawdownCriterion().calculate(series, record).doubleValue();
                if (!Double.isNaN(maxDD)) {
                    sumMaxDD += maxDD;
                    nDDSamples++;
                }
            }
        }

        double avg(double sum, int n) {
            return n > 0 ? sum / n : 0;
        }
    }
}
