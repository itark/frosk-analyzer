package nu.itark.frosk.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseOrderClient;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import nu.itark.frosk.strategies.CryptoIntradayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crypto intraday pipeline orchestrator — the paper-trading loop.
 *
 * <p>Called by {@link nu.itark.frosk.dataset.Scheduler#cryptoIntradaySync()}
 * every 15 minutes around the clock (crypto trades 24/7).
 *
 * <p>Each invocation syncs 15m Coinbase bars for the configured products, runs
 * every registered {@link CryptoIntradayStrategy}, emits BUY/SELL signals to
 * {@code intraday_signal} (the paper-trade audit trail — no orders are placed),
 * and persists {@code FeaturedStrategy} backtest results via
 * {@link StrategyExecutor}. Realized round-trip PnL flows into the intraday
 * portfolio snapshot net of fees.
 */
@Service
@Slf4j
public class CryptoIntradayStrategyRunner {

    private static final int MIN_BARS = 100;
    private static final Duration BAR_DURATION = Duration.ofMinutes(15);
    // Force-close threshold: 120 bars = 30h at 15m — exceeds the longest maxBarsHeld (96)
    private static final long MAX_BARS_FORCE_CLOSE = 120;

    @Autowired
    private CryptoIntradayDataService cryptoIntradayDataService;

    @Autowired
    private List<CryptoIntradayStrategy> cryptoIntradayStrategies;

    @Autowired
    private IntradaySignalRepository signalRepository;

    @Autowired
    private StrategyExecutor strategyExecutor;

    @Autowired(required = false)
    private LiveTradingGate liveTradingGate;

    @Autowired(required = false)
    private CoinbaseOrderClient coinbaseOrderClient;

    @Autowired(required = false)
    private LiveOrderRepository liveOrderRepository;

    @Value("${crypto.live.trading.max.position.eur:500}")
    private BigDecimal maxPositionEur;

    @Value("${crypto.short.excluded.products:}")
    private String shortExcludedProductsRaw;

    @Value("${crypto.emacrossshort.excluded.products:}")
    private String emaCrossShortExcludedProductsRaw;

    @Value("${crypto.vwap.excluded.products:}")
    private String vwapExcludedProductsRaw;

    @Value("${crypto.emacrosslong.excluded.products:}")
    private String emaCrossLongExcludedProductsRaw;

    private Set<String> shortExcludedProducts;
    private Set<String> emaCrossShortExcludedProducts;
    private Set<String> vwapExcludedProducts;
    private Set<String> emaCrossLongExcludedProducts;

    @PostConstruct
    private void initExclusions() {
        shortExcludedProducts = parseExclusions(shortExcludedProductsRaw);
        emaCrossShortExcludedProducts = parseExclusions(emaCrossShortExcludedProductsRaw);
        vwapExcludedProducts = parseExclusions(vwapExcludedProductsRaw);
        emaCrossLongExcludedProducts = parseExclusions(emaCrossLongExcludedProductsRaw);
        log.info("CryptoIntradayStrategyRunner: exclusions loaded — CryptoShort={}, EMACrossShort={}, VWAP={}, EMACrossLong={}",
                shortExcludedProducts, emaCrossShortExcludedProducts, vwapExcludedProducts, emaCrossLongExcludedProducts);
    }

    private Set<String> parseExclusions(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isExcluded(String strategyName, String ticker) {
        if ("CryptoShortIntradayStrategy".equals(strategyName)) {
            return shortExcludedProducts.contains(ticker);
        }
        if ("CryptoEMACrossShortIntradayStrategy".equals(strategyName)) {
            return emaCrossShortExcludedProducts.contains(ticker);
        }
        if ("CryptoVWAPReversionIntradayStrategy".equals(strategyName)) {
            return vwapExcludedProducts.contains(ticker);
        }
        if ("CryptoEMACrossLongIntradayStrategy".equals(strategyName)) {
            return emaCrossLongExcludedProducts.contains(ticker);
        }
        return false;
    }

    public List<String> getStrategyNames() {
        return cryptoIntradayStrategies.stream()
                .map(s -> s.getClass().getSimpleName())
                .toList();
    }

    public void run() {
        log.info("CryptoIntradayStrategyRunner: starting with {} strategies: {}",
                cryptoIntradayStrategies.size(), getStrategyNames());

        Map<Security, BarSeries> allSeries = cryptoIntradayDataService.syncAndBuildAllSeries();
        if (allSeries.isEmpty()) {
            log.warn("CryptoIntradayStrategyRunner: no series available — skipping");
            return;
        }

        int totalSignals = 0;
        List<BarSeries> eligibleSeries = new ArrayList<>();

        for (Map.Entry<Security, BarSeries> entry : allSeries.entrySet()) {
            Security security = entry.getKey();
            BarSeries series = entry.getValue();

            if (series.getBarCount() < MIN_BARS) {
                log.debug("CryptoIntradayStrategyRunner: {} has only {} bars — need {}, skipping",
                        security.getName(), series.getBarCount(), MIN_BARS);
                continue;
            }

            for (CryptoIntradayStrategy cryptoStrategy : cryptoIntradayStrategies) {
                String strategyName = cryptoStrategy.getClass().getSimpleName();
                if (isExcluded(strategyName, security.getName())) {
                    // Even excluded pairs may have a stale open position from before the
                    // exclusion was added. Reconcile it so the DB position is closed.
                    reconcileExcludedIfStaleOpen(strategyName, cryptoStrategy.isShort(), security, series);
                    log.debug("CryptoIntradayStrategyRunner: skipping {} for {} (per-strategy exclusion)",
                            strategyName, security.getName());
                    continue;
                }
                Strategy ta4j = cryptoStrategy.buildStrategy(series);
                totalSignals += evaluateStrategy(ta4j, strategyName, cryptoStrategy.isShort(), security, series);
            }
            eligibleSeries.add(series);
        }

        if (!eligibleSeries.isEmpty()) {
            for (CryptoIntradayStrategy cryptoStrategy : cryptoIntradayStrategies) {
                String strategyName = cryptoStrategy.getClass().getSimpleName();
                strategyExecutor.execute(strategyName, eligibleSeries);
            }
            log.info("CryptoIntradayStrategyRunner: FeaturedStrategy updated for {} securities x {} strategies",
                    eligibleSeries.size(), cryptoIntradayStrategies.size());
        }

        log.info("CryptoIntradayStrategyRunner: completed — {} signals emitted across {} securities",
                totalSignals, allSeries.size());
    }

    private int evaluateStrategy(Strategy ta4jStrategy, String strategyName, boolean isShort,
                                  Security security, BarSeries series) {
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(ta4jStrategy);

        int lastIndex = series.getEndIndex();
        String enterSignal = isShort ? "SHRT" : "BUY";
        String exitSignal  = isShort ? "COVR" : "SELL";

        // ── Real-world reconcile ──────────────────────────────────────────────
        // The backtest is stateless: it re-runs the full history each tick and may
        // re-enter at a different bar than the original real-world signal. Three cases
        // can leave a real-world SHRT/BUY without a matching COVR/SELL:
        //   (a) backtest closed mid-series → isNew() at last bar → no exit emitted
        //   (b) backtest re-entered at later bar → isOpened() but MaxBarsHeld counts
        //       from the new (wrong) entry index → shouldExit() never fires for old position
        //   (c) entry bar no longer in series (very old) → (b) applies indefinitely
        // Fix: if the real-world position is unmatched AND the backtest agrees to exit
        // (or the position is older than MAX_BARS_FORCE_CLOSE), emit the exit now.
        if (hasRealWorldOpenPosition(strategyName, security.getName(), isShort)) {
            boolean backtestClosed    = tradingRecord.getCurrentPosition().isNew();
            boolean backtestShouldExit = tradingRecord.getCurrentPosition().isOpened()
                                         && ta4jStrategy.shouldExit(lastIndex, tradingRecord);
            boolean realWorldExpired  = isRealWorldPositionExpired(strategyName, security.getName(), isShort);
            if (backtestClosed || backtestShouldExit || realWorldExpired) {
                log.warn("CryptoIntradayStrategyRunner: stale/expired open position — emitting {} for {}/{} "
                        + "(backtestClosed={}, backtestShouldExit={}, realWorldExpired={})",
                        exitSignal, strategyName, security.getName(), backtestClosed, backtestShouldExit, realWorldExpired);
                emitSignal(exitSignal, strategyName, security.getName(), series, lastIndex);
                return 1;
            }
            return 0;
        }

        if (tradingRecord.getCurrentPosition().isNew()) {
            if (ta4jStrategy.shouldEnter(lastIndex, tradingRecord)) {
                emitSignal(enterSignal, strategyName, security.getName(), series, lastIndex);
                return 1;
            }
        } else if (tradingRecord.getCurrentPosition().isOpened()) {
            if (ta4jStrategy.shouldExit(lastIndex, tradingRecord)) {
                emitSignal(exitSignal, strategyName, security.getName(), series, lastIndex);
                return 1;
            }
        }
        return 0;
    }

    private boolean hasRealWorldOpenPosition(String strategyName, String ticker, boolean isShort) {
        String entryType = isShort ? "SHRT" : "BUY";
        String exitType  = isShort ? "COVR" : "SELL";
        Optional<IntradaySignal> latestEntry = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, entryType);
        if (latestEntry.isEmpty()) return false;
        Optional<IntradaySignal> latestExit = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, exitType);
        // Open if the most recent entry is newer than the most recent exit (or no exit exists).
        // Comparing timestamps is robust against orphan exits that pre-date any entry.
        return latestExit.isEmpty()
                || latestEntry.get().getSignalTimestamp() > latestExit.get().getSignalTimestamp();
    }

    private boolean isRealWorldPositionExpired(String strategyName, String ticker, boolean isShort) {
        String entryType = isShort ? "SHRT" : "BUY";
        Optional<IntradaySignal> latestEntry = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, entryType);
        if (latestEntry.isEmpty()) return false;
        long entryEpoch = latestEntry.get().getSignalTimestamp();
        long nowEpoch   = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        long elapsedBars = (nowEpoch - entryEpoch) / BAR_DURATION.getSeconds();
        return elapsedBars >= MAX_BARS_FORCE_CLOSE;
    }

    /**
     * Force-closes a stale open position for an excluded (ticker, strategy) pair.
     * Called before the {@code continue} so excluded pairs don't stay open forever
     * when the exclusion was added after the position was entered.
     */
    private void reconcileExcludedIfStaleOpen(String strategyName, boolean isShort,
                                               Security security, BarSeries series) {
        if (!hasRealWorldOpenPosition(strategyName, security.getName(), isShort)) return;
        if (!isRealWorldPositionExpired(strategyName, security.getName(), isShort)) return;
        String exitSignal = isShort ? "COVR" : "SELL";
        log.warn("CryptoIntradayStrategyRunner: excluded pair {}/{} has stale open position — force-closing with {}",
                strategyName, security.getName(), exitSignal);
        emitSignal(exitSignal, strategyName, security.getName(), series, series.getEndIndex());
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

        log.info("CryptoIntradayStrategyRunner: {} {} — ticker={}, bar={}, close={}",
                strategyName, signalType, ticker, barStartEpoch, signal.getClosePrice());

        dispatchLiveOrder(signalType, strategyName, ticker, signal.getClosePrice(), signal);
    }

    /**
     * Routes BUY/SELL signals to Coinbase for long-only strategies.
     * SHRT and COVR signals (short strategies) are intentionally skipped.
     * Marks {@code signal.live = true} when the order is successfully filled.
     */
    private void dispatchLiveOrder(String signalType, String strategyName, String ticker,
                                   BigDecimal closePrice, IntradaySignal signal) {
        if (liveTradingGate == null || coinbaseOrderClient == null || liveOrderRepository == null) return;
        if (!"BUY".equals(signalType) && !"SELL".equals(signalType)) return; // skip SHRT/COVR

        if ("BUY".equals(signalType)) {
            dispatchBuy(strategyName, ticker, closePrice, signal);
        } else {
            dispatchSell(strategyName, ticker, signal);
        }
    }

    private void dispatchBuy(String strategyName, String ticker, BigDecimal closePrice, IntradaySignal signal) {
        if (!liveTradingGate.canTrade(ticker, maxPositionEur)) return;

        OrderResponse resp = coinbaseOrderClient.placeBuyOrder(ticker, maxPositionEur);

        LiveOrder order = new LiveOrder();
        order.setTicker(ticker);
        order.setSide("BUY");
        order.setStrategyName(strategyName);
        order.setEurAmount(maxPositionEur);
        order.setCoinbaseOrderId(resp.getOrderId());
        order.setClientOrderId(resp.getClientOrderId());

        if ("PENDING".equals(resp.getStatus()) || "FILLED".equals(resp.getStatus())) {
            order.setStatus("FILLED");
            order.setFilledPrice(resp.getAverageFilledPrice() != null ? resp.getAverageFilledPrice() : closePrice);
            order.setFilledQuantity(resp.getFilledSize());
            order.setFilledAt(LocalDateTime.now());
            signal.setLive(true);
            signalRepository.save(signal);
            log.info("LIVE ORDER: BUY {} {} @ {}EUR (orderId={})",
                    ticker, resp.getFilledSize(), resp.getAverageFilledPrice(), resp.getOrderId());
        } else {
            order.setStatus("FAILED");
            order.setErrorMessage(resp.getErrorMessage());
            log.warn("LIVE ORDER FAILED: BUY {} — {}", ticker, resp.getErrorMessage());
        }
        liveOrderRepository.save(order);
    }

    private void dispatchSell(String strategyName, String ticker, IntradaySignal signal) {
        LocalDateTime since = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay();
        Optional<LiveOrder> openBuy = liveOrderRepository
                .findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(
                        ticker, strategyName, "BUY", "FILLED");

        if (openBuy.isEmpty()) {
            log.debug("CryptoIntradayStrategyRunner: SELL signal for {} / {} but no open BUY position — skip",
                    ticker, strategyName);
            return;
        }
        LiveOrder buyOrder = openBuy.get();
        if (buyOrder.getFilledQuantity() == null || buyOrder.getFilledQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("CryptoIntradayStrategyRunner: open BUY for {} has no filled quantity — skip", ticker);
            return;
        }

        OrderResponse resp = coinbaseOrderClient.placeSellOrder(ticker, buyOrder.getFilledQuantity());

        LiveOrder sellOrder = new LiveOrder();
        sellOrder.setTicker(ticker);
        sellOrder.setSide("SELL");
        sellOrder.setStrategyName(strategyName);
        sellOrder.setFilledQuantity(buyOrder.getFilledQuantity());
        sellOrder.setCoinbaseOrderId(resp.getOrderId());
        sellOrder.setClientOrderId(resp.getClientOrderId());

        if ("PENDING".equals(resp.getStatus()) || "FILLED".equals(resp.getStatus())) {
            sellOrder.setStatus("FILLED");
            BigDecimal sellPrice = resp.getAverageFilledPrice();
            sellOrder.setFilledPrice(sellPrice);
            sellOrder.setFilledAt(LocalDateTime.now());

            if (sellPrice != null && buyOrder.getFilledPrice() != null) {
                BigDecimal pnl = sellPrice.subtract(buyOrder.getFilledPrice())
                        .multiply(buyOrder.getFilledQuantity());
                sellOrder.setRealizedPnlEur(pnl);
            }
            signal.setLive(true);
            signalRepository.save(signal);
            log.info("LIVE ORDER: SELL {} {} @ {}EUR (pnl={}EUR, orderId={})",
                    ticker, buyOrder.getFilledQuantity(), sellPrice,
                    sellOrder.getRealizedPnlEur(), resp.getOrderId());

            // Mark the matched BUY as consumed so it won't be matched again
            buyOrder.setStatus("CLOSED");
            liveOrderRepository.save(buyOrder);
        } else {
            sellOrder.setStatus("FAILED");
            sellOrder.setErrorMessage(resp.getErrorMessage());
            log.warn("LIVE ORDER FAILED: SELL {} — {}", ticker, resp.getErrorMessage());
        }
        liveOrderRepository.save(sellOrder);
    }
}
