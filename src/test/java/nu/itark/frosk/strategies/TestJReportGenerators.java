package nu.itark.frosk.strategies;


import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.BarSeriesService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.reports.*;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJReportGenerators extends BaseIntegrationTest {

    @Autowired
    BarSeriesService barSeriesService;

    BarSeries series;
    Strategy strategy;
    BarSeriesManager seriesManager;
    @BeforeEach
    private void setup(){
        series = barSeriesService.getDataSet("FIL-EUR", false, false);
        strategy = new HaramiStrategy(series).buildStrategy();
        seriesManager = new BarSeriesManager(series);
    }

    @Test
    public void runPrg() {
        PerformanceReportGenerator performanceReportGenerator = new  PerformanceReportGenerator();
        TradingRecord tradingRecord = seriesManager.run(strategy);
        final PerformanceReport performanceReport = performanceReportGenerator.generate(strategy, tradingRecord, series);

        System.out.println("performanceReport="+ ReflectionToStringBuilder.toString(performanceReport));
    }

    @Test
    public void runPsg() {
        PositionStatsReportGenerator performanceReportGenerator = new  PositionStatsReportGenerator();
        TradingRecord tradingRecord = seriesManager.run(strategy);
        final PositionStatsReport performanceReport = performanceReportGenerator.generate(strategy, tradingRecord, series);

        System.out.println("performanceReport="+ ReflectionToStringBuilder.toString(performanceReport));
    }


    @Test
    public void runTsg() {
        TradingStatementGenerator performanceReportGenerator = new  TradingStatementGenerator();
        TradingRecord tradingRecord = seriesManager.run(strategy);
        final TradingStatement performanceReport = performanceReportGenerator.generate(strategy, tradingRecord, series);

        System.out.println("performanceReport="+ ReflectionToStringBuilder.toString(performanceReport.getPerformanceReport()));
        System.out.println("positionStatsReport="+ ReflectionToStringBuilder.toString(performanceReport.getPositionStatsReport()));

    }

}
