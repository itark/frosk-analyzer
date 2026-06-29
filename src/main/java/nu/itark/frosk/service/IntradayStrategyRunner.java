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
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private static final Duration BAR_DURATION = Duration.ofMinutes(15);
    // Force-close threshold: 120 bars = 30h wall-clock at 15m — far exceeds the
    // longest per-strategy maxBarsHeld (20 bars for GapReversal) yet allows
    // overnight positions to survive until the next morning's Tier-0 run.
    private static final long MAX_BARS_FORCE_CLOSE = 120;

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

        // Close orphaned BUY signals from strategies that no longer exist
        Set<String> activeStrategyNames = intradayStrategies.stream()
                .map(s -> s.getClass().getSimpleName())
                .collect(Collectors.toSet());
        reconcileDeadStrategyOrphans(activeStrategyNames, allSeries);

        int totalSignals = 0;
        List<BarSeries> eligibleSeries = new ArrayList<>();
        Map<String, Integer> signalsByStrategy = new HashMap<>();

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
                int signals = evaluateStrategy(ta4j, strategyName, security, series);
                totalSignals += signals;
                signalsByStrategy.merge(strategyName, signals, Integer::sum);
            }
            eligibleSeries.add(series);
        }

        if (!eligibleSeries.isEmpty()) {
            List<String> updated = new ArrayList<>();
            for (IntradayStrategy intradayStrategy : intradayStrategies) {
                String strategyName = intradayStrategy.getClass().getSimpleName();
                if (signalsByStrategy.getOrDefault(strategyName, 0) > 0) {
                    strategyExecutor.execute(strategyName, eligibleSeries);
                    updated.add(strategyName);
                }
            }
            if (!updated.isEmpty()) {
                log.info("IntradayStrategyRunner: FeaturedStrategy updated for {} securities x {} strategies: {}",
                        eligibleSeries.size(), updated.size(), updated);
            }
        }

        log.info("IntradayStrategyRunner: completed — {} signals emitted across {} securities",
                totalSignals, allSeries.size());
    }

    private int evaluateStrategy(Strategy ta4jStrategy, String strategyName,
                                  Security security, BarSeries series) {
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(ta4jStrategy);

        int lastIndex = series.getEndIndex();

        // ── Real-world reconcile ──────────────────────────────────────────────
        // The backtest is stateless: it re-runs full history each tick and may
        // re-enter at a different bar than the original real-world signal. Three
        // cases can leave a real-world BUY without a matching SELL:
        //   (a) backtest closed mid-series → isNew() at last bar → no exit emitted
        //   (b) backtest re-entered at a later bar → MaxBarsHeld counts from the new
        //       (wrong) entry index → shouldExit() never fires for the old position
        //   (c) entry bar no longer in series (very old) → (b) applies indefinitely
        // Fix: if the real-world position is unmatched AND the backtest agrees to exit
        // (or the position is older than MAX_BARS_FORCE_CLOSE), emit SELL now.
        if (hasRealWorldOpenPosition(strategyName, security.getName())) {
            boolean backtestClosed     = tradingRecord.getCurrentPosition().isNew();
            boolean backtestShouldExit = tradingRecord.getCurrentPosition().isOpened()
                                         && ta4jStrategy.shouldExit(lastIndex, tradingRecord);
            boolean realWorldExpired   = isRealWorldPositionExpired(strategyName, security.getName());
            if (backtestClosed || backtestShouldExit || realWorldExpired) {
                log.warn("IntradayStrategyRunner: stale/expired open position — emitting SELL for {}/{} "
                        + "(backtestClosed={}, backtestShouldExit={}, realWorldExpired={})",
                        strategyName, security.getName(), backtestClosed, backtestShouldExit, realWorldExpired);
                emitSignal("SELL", strategyName, security.getName(), series, lastIndex);
                return 1;
            }
            return 0;
        }

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

    /**
     * Returns true when the most recent BUY for this (strategy, ticker) pair has
     * no matching SELL — i.e. there is a real-world open position in the DB.
     */
    private boolean hasRealWorldOpenPosition(String strategyName, String ticker) {
        Optional<IntradaySignal> latestBuy = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, "BUY");
        if (latestBuy.isEmpty()) return false;
        Optional<IntradaySignal> latestSell = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, "SELL");
        return latestSell.isEmpty()
                || latestBuy.get().getSignalTimestamp() > latestSell.get().getSignalTimestamp();
    }

    /**
     * Returns true when the real-world open position is older than
     * MAX_BARS_FORCE_CLOSE * 15 minutes of wall-clock time.
     */
    private boolean isRealWorldPositionExpired(String strategyName, String ticker) {
        Optional<IntradaySignal> latestBuy = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, "BUY");
        if (latestBuy.isEmpty()) return false;
        long entryEpoch  = latestBuy.get().getSignalTimestamp();
        long nowEpoch    = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        long elapsedBars = (nowEpoch - entryEpoch) / BAR_DURATION.getSeconds();
        return elapsedBars >= MAX_BARS_FORCE_CLOSE;
    }

    /**
     * Force-closes open BUY signals for strategy names that no longer exist as
     * registered beans. These orphans can never be closed by normal evaluation.
     */
    private void reconcileDeadStrategyOrphans(Set<String> activeStrategyNames,
                                               Map<Security, BarSeries> allSeries) {
        List<String> knownStrategies = signalRepository.findDistinctStrategyNames();
        Set<String> deadStrategies = knownStrategies.stream()
                .filter(s -> !activeStrategyNames.contains(s))
                .collect(Collectors.toSet());
        if (deadStrategies.isEmpty()) return;

        Map<String, BarSeries> tickerToSeries = new HashMap<>();
        for (Map.Entry<Security, BarSeries> e : allSeries.entrySet()) {
            tickerToSeries.put(e.getKey().getName(), e.getValue());
        }

        List<String> allTickers = signalRepository.findDistinctTickers();
        for (String deadStrategy : deadStrategies) {
            for (String ticker : allTickers) {
                if (!hasRealWorldOpenPosition(deadStrategy, ticker)) continue;
                BarSeries series = tickerToSeries.get(ticker);
                if (series == null) {
                    log.warn("IntradayStrategyRunner: dead-strategy orphan {}/{} — no bar series, cannot force-close",
                            deadStrategy, ticker);
                    continue;
                }
                log.warn("IntradayStrategyRunner: force-closing orphaned position for removed strategy {}/{}",
                        deadStrategy, ticker);
                emitSignal("SELL", deadStrategy, ticker, series, series.getEndIndex());
            }
        }
    }

    private void emitSignal(String signalType, String strategyName, String ticker,
                            BarSeries series, int index) {
        Bar bar = series.getBar(index);
        ZonedDateTime barEnd = bar.getEndTime();
        long barStartEpoch = barEnd.toEpochSecond() - BAR_DURATION.getSeconds();

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
