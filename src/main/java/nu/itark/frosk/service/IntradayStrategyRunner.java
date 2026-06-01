package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.strategies.OMX30IntradayMomentumStrategy;
import nu.itark.frosk.strategies.RunawayGAPIntradayStrategy;
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
 * <p>Each invocation fetches 15-minute bars for all OMX30 constituent stocks,
 * runs {@link OMX30IntradayMomentumStrategy} on each, emits BUY/SELL
 * signals to the {@code intraday_signal} table, and persists
 * {@code FeaturedStrategy} backtest results via {@link StrategyExecutor}.
 */
@Service
@Slf4j
public class IntradayStrategyRunner {

    private static final int MIN_BARS = 30;
    private static final String MOMENTUM_STRATEGY = "OMX30IntradayMomentumStrategy";
    private static final String GAP_STRATEGY = "RunawayGAPIntradayStrategy";

    @Autowired
    private IntradayDataService intradayDataService;

    @Autowired
    private OMX30IntradayMomentumStrategy momentumStrategy;

    @Autowired
    private RunawayGAPIntradayStrategy gapStrategy;

    @Autowired
    private IntradaySignalRepository signalRepository;

    @Autowired
    private StrategyExecutor strategyExecutor;

    public void run() {
        log.info("IntradayStrategyRunner: starting");

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

            totalSignals += evaluateStrategy(momentumStrategy.buildStrategy(series),
                    MOMENTUM_STRATEGY, security, series);
            totalSignals += evaluateStrategy(gapStrategy.buildStrategy(series),
                    GAP_STRATEGY, security, series);
            eligibleSeries.add(series);
        }

        if (!eligibleSeries.isEmpty()) {
            strategyExecutor.execute(MOMENTUM_STRATEGY, eligibleSeries);
            strategyExecutor.execute(GAP_STRATEGY, eligibleSeries);
            log.info("IntradayStrategyRunner: FeaturedStrategy updated for {} securities × 2 strategies",
                    eligibleSeries.size());
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

        if (signalRepository.existsByTickerAndSignalTimestampAndSignalType(
                ticker, barStartEpoch, signalType)) {
            return;
        }

        BigDecimal ema9 = null, ema21 = null, rsi7 = null;
        if (MOMENTUM_STRATEGY.equals(strategyName)) {
            Double ef  = momentumStrategy.getEmaFastAt(index);
            Double es  = momentumStrategy.getEmaSlowAt(index);
            Double r   = momentumStrategy.getRsiAt(index);
            ema9  = ef != null ? toBd(ef) : null;
            ema21 = es != null ? toBd(es) : null;
            rsi7  = r  != null ? toBd(r)  : null;
        }

        IntradaySignal signal = new IntradaySignal(
                ticker, barStartEpoch, signalType,
                toBd(bar.getClosePrice().doubleValue()),
                ema9, ema21, rsi7
        );
        signalRepository.save(signal);

        log.info("IntradayStrategyRunner: {} {} — ticker={}, bar={}, close={}",
                strategyName, signalType, ticker, barStartEpoch, signal.getClosePrice());
    }

    private static BigDecimal toBd(double value) {
        return BigDecimal.valueOf(value);
    }
}
