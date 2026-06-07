package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.strategies.IntradayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tier-0 intraday pipeline orchestrator.
 *
 * <p>Called by {@link nu.itark.frosk.dataset.Scheduler#tier0IntradaySync()}
 * every 10 minutes during Stockholm market hours (09:00–17:30 CET, Mon–Fri).
 *
 * <p>Each invocation fetches intraday bars for all configured securities,
 * runs every registered {@link IntradayStrategy} on each, emits BUY/SELL
 * signals to the {@code intraday_signal} table, and persists
 * {@code FeaturedStrategy} backtest results via {@link StrategyExecutor}.
 */
@Service
@Slf4j
public class IntradayStrategyRunner {

    private static final int MIN_BARS = 30;

    @Autowired
    private IntradayDataService intradayDataService;

    @Autowired
    private List<IntradayStrategy> intradayStrategies;

    @Autowired
    private IntradaySignalRepository signalRepository;

    @Autowired
    private StrategyExecutor strategyExecutor;

    public List<String> getStrategyNames() {
        return intradayStrategies.stream()
                .map(s -> s.getClass().getSimpleName())
                .toList();
    }

    public void run() {
        log.info("IntradayStrategyRunner: starting with {} strategies: {}",
                intradayStrategies.size(), getStrategyNames());

        Map<Security, BarSeries> allSeries = intradayDataService.syncAndBuildAllSeries();
        if (allSeries.isEmpty()) {
            log.warn("IntradayStrategyRunner: no series available — skipping");
            return;
        }

        int totalSignals = 0;
        List<BarSeries> eligibleSeries = new ArrayList<>();

        for (Map.Entry<Security, BarSeries> entry : allSeries.entrySet()) {
            Security security = entry.getKey();
            BarSeries series = entry.getValue();

            if (series.getBarCount() < MIN_BARS) {
                log.debug("IntradayStrategyRunner: {} has only {} bars — need {}, skipping",
                        security.getName(), series.getBarCount(), MIN_BARS);
                continue;
            }

            for (IntradayStrategy intradayStrategy : intradayStrategies) {
                String strategyName = intradayStrategy.getClass().getSimpleName();
                Strategy ta4j = intradayStrategy.buildStrategy(series);
                totalSignals += evaluateStrategy(ta4j, strategyName, security, series);
            }
            eligibleSeries.add(series);
        }

        if (!eligibleSeries.isEmpty()) {
            for (IntradayStrategy intradayStrategy : intradayStrategies) {
                String strategyName = intradayStrategy.getClass().getSimpleName();
                strategyExecutor.execute(strategyName, eligibleSeries);
            }
            log.info("IntradayStrategyRunner: FeaturedStrategy updated for {} securities x {} strategies",
                    eligibleSeries.size(), intradayStrategies.size());
        }

        log.info("IntradayStrategyRunner: completed — {} signals emitted across {} securities",
                totalSignals, allSeries.size());
    }

    private int evaluateStrategy(Strategy ta4jStrategy, String strategyName,
                                  Security security, BarSeries series) {
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(ta4jStrategy);

        int lastIndex = series.getEndIndex();

        if (tradingRecord.getCurrentPosition().isNew()) {
            if (ta4jStrategy.shouldEnter(lastIndex, tradingRecord)) {
                emitSignal("BUY", strategyName, security.getName(), series, lastIndex);
                return 1;
            }
        } else if (tradingRecord.getCurrentPosition().isOpened()) {
            if (ta4jStrategy.shouldExit(lastIndex, tradingRecord)) {
                emitSignal("SELL", strategyName, security.getName(), series, lastIndex);
                return 1;
            }
        }
        return 0;
    }

    private void emitSignal(String signalType, String strategyName, String ticker,
                            BarSeries series, int index) {
        Bar bar = series.getBar(index);
        ZonedDateTime barEnd = bar.getEndTime();
        long barStartEpoch = barEnd.toEpochSecond() - 300;

        if (signalRepository.existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
                strategyName, ticker, barStartEpoch, signalType)) {
            return;
        }

        IntradaySignal signal = new IntradaySignal(
                strategyName, ticker, barStartEpoch, signalType,
                BigDecimal.valueOf(bar.getClosePrice().doubleValue())
        );
        signalRepository.save(signal);

        log.info("IntradayStrategyRunner: {} {} — ticker={}, bar={}, close={}",
                strategyName, signalType, ticker, barStartEpoch, signal.getClosePrice());
    }
}
