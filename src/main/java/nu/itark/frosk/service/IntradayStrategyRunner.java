package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.strategies.OMX30IntradayMomentumStrategy;
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
 * <p>Each invocation fetches 5-minute bars for all OMX30 constituent stocks,
 * runs {@link OMX30IntradayMomentumStrategy} on each, emits BUY/SELL
 * signals to the {@code intraday_signal} table, and persists
 * {@code FeaturedStrategy} backtest results via {@link StrategyExecutor}.
 */
@Service
@Slf4j
public class IntradayStrategyRunner {

    private static final int MIN_BARS = 30;
    private static final String STRATEGY_NAME = "OMX30IntradayMomentumStrategy";

    @Autowired
    private IntradayDataService intradayDataService;

    @Autowired
    private OMX30IntradayMomentumStrategy strategy;

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

            totalSignals += evaluateSecurity(security, series);
            eligibleSeries.add(series);
        }

        if (!eligibleSeries.isEmpty()) {
            strategyExecutor.execute(STRATEGY_NAME, eligibleSeries);
            log.info("IntradayStrategyRunner: FeaturedStrategy updated for {} securities", eligibleSeries.size());
        }

        log.info("IntradayStrategyRunner: completed — {} signals emitted across {} securities",
                totalSignals, allSeries.size());
    }

    private int evaluateSecurity(Security security, BarSeries series) {
        Strategy ta4jStrategy = strategy.buildStrategy(series);
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(ta4jStrategy);

        int lastIndex = series.getEndIndex();
        int signals = 0;

        if (tradingRecord.getCurrentPosition().isNew()) {
            if (ta4jStrategy.shouldEnter(lastIndex, tradingRecord)) {
                emitSignal("BUY", security.getName(), series, lastIndex);
                signals++;
            }
        } else if (tradingRecord.getCurrentPosition().isOpened()) {
            if (ta4jStrategy.shouldExit(lastIndex, tradingRecord)) {
                emitSignal("SELL", security.getName(), series, lastIndex);
                signals++;
            }
        }

        return signals;
    }

    private void emitSignal(String signalType, String ticker, BarSeries series, int index) {
        Bar bar = series.getBar(index);
        ZonedDateTime barEnd = bar.getEndTime();
        long barStartEpoch = barEnd.toEpochSecond() - 300;

        if (signalRepository.existsByTickerAndSignalTimestampAndSignalType(
                ticker, barStartEpoch, signalType)) {
            log.debug("IntradayStrategyRunner: signal {}/{}/{} already stored — skipping",
                    ticker, barStartEpoch, signalType);
            return;
        }

        Double ema9  = strategy.getEma9At(index);
        Double ema21 = strategy.getEma21At(index);
        Double rsi7  = strategy.getRsi7At(index);

        IntradaySignal signal = new IntradaySignal(
                ticker,
                barStartEpoch,
                signalType,
                toBd(bar.getClosePrice().doubleValue()),
                ema9  != null ? toBd(ema9)  : null,
                ema21 != null ? toBd(ema21) : null,
                rsi7  != null ? toBd(rsi7)  : null
        );

        signalRepository.save(signal);

        log.info("IntradayStrategyRunner: {} signal — ticker={}, bar={}, close={}, EMA9={}, EMA21={}, RSI7={}",
                signalType, ticker, barStartEpoch,
                signal.getClosePrice(), signal.getEma9(), signal.getEma21(), signal.getRsi7());
    }

    private static BigDecimal toBd(double value) {
        return BigDecimal.valueOf(value);
    }
}
