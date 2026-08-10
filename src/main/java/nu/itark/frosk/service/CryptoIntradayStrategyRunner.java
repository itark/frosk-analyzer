package nu.itark.frosk.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.StrategyExecutor;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.crypto.coinbase.model.ProductBook;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseOrderClient;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.crypto.livetrading.OrderResponse;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import nu.itark.frosk.strategies.CryptoIntradayStrategy;
import nu.itark.frosk.strategies.ISignalStrength;
import nu.itark.frosk.strategies.SignalStrength;
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

    @Autowired(required = false)
    private CryptoPaperTradingService paperTradingService;

    @Autowired
    private ProductService productService;

    // Global switches, not per-product lists — Coinbase doesn't support short
    // selling, so these stay off regardless of how crypto.intraday.products grows.
    @Value("${crypto.short.enabled:false}")
    private boolean shortEnabled;

    @Value("${crypto.emacrossshort.enabled:false}")
    private boolean emaCrossShortEnabled;

    // Kill switches for the two long strategies whose gross edge is negative
    // BEFORE any fee — no fee tier or venue makes them profitable. Default true
    // so the code stays neutral; the crypto profile turns them off.
    @Value("${crypto.emacrosslong.enabled:true}")
    private boolean emaCrossLongEnabled;

    @Value("${crypto.breakout.enabled:true}")
    private boolean breakoutEnabled;

    /**
     * PDH/PDL liquidity sweep. Runs to ACCUMULATE FORWARD DATA for the pre-registered
     * test in ~/itark/PREREG_liquidity_sweep_15m.md — it is not a validated strategy.
     * Its paper P&L must not be inspected before that document's data gate is met;
     * peeking early turns a pre-registered test into an unregistered one.
     */
    @Value("${crypto.sweep.enabled:true}")
    private boolean sweepEnabled;

    /**
     * Bars before a position in an EXCLUDED/disabled (ticker, strategy) pair is
     * force-closed. Separate from {@link #MAX_BARS_FORCE_CLOSE}, which still governs
     * live strategies — lowering that one would force-close healthy open positions
     * on strategies that are running, producing same-bar fee-only exits. Defaults to
     * the same 120 bars (30h); lower it temporarily to flush a strategy you just
     * disabled. Never set below 1: a position entered on the current bar would be
     * closed at its own entry price, which is a guaranteed fee-only loss.
     */
    @Value("${crypto.excluded.force.close.bars:120}")
    private long excludedForceCloseBars;

    /** Capture the bid/ask spread on every emitted signal (one REST call per signal). */
    @Value("${crypto.spread.logging.enabled:true}")
    private boolean spreadLoggingEnabled;

    @Value("${crypto.vwap.excluded.products:}")
    private String vwapExcludedProductsRaw;

    @Value("${crypto.emacrosslong.excluded.products:}")
    private String emaCrossLongExcludedProductsRaw;

    private Set<String> vwapExcludedProducts;
    private Set<String> emaCrossLongExcludedProducts;

    @PostConstruct
    private void initExclusions() {
        vwapExcludedProducts = parseExclusions(vwapExcludedProductsRaw);
        emaCrossLongExcludedProducts = parseExclusions(emaCrossLongExcludedProductsRaw);
        log.info("CryptoIntradayStrategyRunner: enabled — Short={}, EMACrossShort={}, EMACrossLong={}, "
                        + "RangeBreakout={}, LiquiditySweep={}; VWAP exclusions={}, EMACrossLong exclusions={}",
                shortEnabled, emaCrossShortEnabled, emaCrossLongEnabled, breakoutEnabled, sweepEnabled,
                vwapExcludedProducts, emaCrossLongExcludedProducts);
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
            return !shortEnabled;
        }
        if ("CryptoEMACrossShortIntradayStrategy".equals(strategyName)) {
            return !emaCrossShortEnabled;
        }
        if ("CryptoRangeBreakoutIntradayStrategy".equals(strategyName)) {
            return !breakoutEnabled;
        }
        if ("CryptoLiquiditySweepIntradayStrategy".equals(strategyName)) {
            return !sweepEnabled;
        }
        if ("CryptoVWAPReversionIntradayStrategy".equals(strategyName)) {
            return vwapExcludedProducts.contains(ticker);
        }
        if ("CryptoEMACrossLongIntradayStrategy".equals(strategyName)) {
            return !emaCrossLongEnabled || emaCrossLongExcludedProducts.contains(ticker);
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
                ISignalStrength strengthSource = cryptoStrategy instanceof ISignalStrength
                        ? (ISignalStrength) cryptoStrategy : null;
                totalSignals += evaluateStrategy(ta4j, strategyName, cryptoStrategy.isShort(), security, series, strengthSource);
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
                                  Security security, BarSeries series, ISignalStrength strengthSource) {
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
            // Never reconcile a position that was opened on the current bar. The
            // backtest is stateless (re-simulated from scratch every call), so two
            // invocations against the same not-yet-advanced bar (e.g. a restart
            // shortly after a cron tick — FroskStartupApplicationListener kicks
            // syncCryptoIntraday() once at startup, independent of the 15m cron)
            // can disagree with each other. If that happens here, the position
            // would be entered and immediately closed on the same bar/price — a
            // guaranteed fee-only loss with zero real market exposure. Confirmed
            // in production data (intraday_signal ids 20252-20305 and 15 other
            // occurrences): wait for a genuinely new bar before reconciling.
            long currentBarEpoch = barStartEpoch(series, lastIndex);
            Optional<IntradaySignal> latestEntry = latestRealWorldEntry(strategyName, security.getName(), isShort);
            if (latestEntry.isPresent() && latestEntry.get().getSignalTimestamp() == currentBarEpoch) {
                log.debug("CryptoIntradayStrategyRunner: skip reconcile for {}/{} — entry is on the current bar, no new bar since entry",
                        strategyName, security.getName());
                return 0;
            }

            boolean backtestClosed    = tradingRecord.getCurrentPosition().isNew();
            boolean backtestShouldExit = tradingRecord.getCurrentPosition().isOpened()
                                         && ta4jStrategy.shouldExit(lastIndex, tradingRecord);
            boolean realWorldExpired  = isRealWorldPositionExpired(strategyName, security.getName(), isShort);
            if (backtestClosed || backtestShouldExit || realWorldExpired) {
                log.warn("CryptoIntradayStrategyRunner: stale/expired open position — emitting {} for {}/{} "
                        + "(backtestClosed={}, backtestShouldExit={}, realWorldExpired={})",
                        exitSignal, strategyName, security.getName(), backtestClosed, backtestShouldExit, realWorldExpired);
                emitSignal(exitSignal, strategyName, security.getName(), series, lastIndex, null);
                return 1;
            }
            return 0;
        }

        if (tradingRecord.getCurrentPosition().isNew()) {
            if (ta4jStrategy.shouldEnter(lastIndex, tradingRecord)) {
                SignalStrength strength = strengthSource != null
                        ? strengthSource.getSignalStrength(lastIndex) : null;
                emitSignal(enterSignal, strategyName, security.getName(), series, lastIndex, strength);
                return 1;
            }
        } else if (tradingRecord.getCurrentPosition().isOpened()) {
            if (ta4jStrategy.shouldExit(lastIndex, tradingRecord)) {
                emitSignal(exitSignal, strategyName, security.getName(), series, lastIndex, null);
                return 1;
            }
        }
        return 0;
    }

    private boolean hasRealWorldOpenPosition(String strategyName, String ticker, boolean isShort) {
        String exitType = isShort ? "COVR" : "SELL";
        Optional<IntradaySignal> latestEntry = latestRealWorldEntry(strategyName, ticker, isShort);
        if (latestEntry.isEmpty()) return false;
        Optional<IntradaySignal> latestExit = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, exitType);
        // Open if the most recent entry is newer than the most recent exit (or no exit exists).
        // Comparing timestamps is robust against orphan exits that pre-date any entry.
        return latestExit.isEmpty()
                || latestEntry.get().getSignalTimestamp() > latestExit.get().getSignalTimestamp();
    }

    private Optional<IntradaySignal> latestRealWorldEntry(String strategyName, String ticker, boolean isShort) {
        String entryType = isShort ? "SHRT" : "BUY";
        return signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(strategyName, ticker, entryType);
    }

    private long barStartEpoch(BarSeries series, int index) {
        Bar bar = series.getBar(index);
        return bar.getEndTime().toEpochSecond() - BAR_DURATION.getSeconds();
    }

    private boolean isRealWorldPositionExpired(String strategyName, String ticker, boolean isShort) {
        return isRealWorldPositionExpired(strategyName, ticker, isShort, MAX_BARS_FORCE_CLOSE);
    }

    private boolean isRealWorldPositionExpired(String strategyName, String ticker, boolean isShort,
                                               long maxBars) {
        Optional<IntradaySignal> latestEntry = latestRealWorldEntry(strategyName, ticker, isShort);
        if (latestEntry.isEmpty()) return false;
        long entryEpoch = latestEntry.get().getSignalTimestamp();
        long nowEpoch   = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        long elapsedBars = (nowEpoch - entryEpoch) / BAR_DURATION.getSeconds();
        return elapsedBars >= maxBars;
    }

    /**
     * Force-closes a stale open position for an excluded (ticker, strategy) pair.
     * Called before the {@code continue} so excluded pairs don't stay open forever
     * when the exclusion was added after the position was entered.
     */
    private void reconcileExcludedIfStaleOpen(String strategyName, boolean isShort,
                                               Security security, BarSeries series) {
        if (!hasRealWorldOpenPosition(strategyName, security.getName(), isShort)) return;
        if (!isRealWorldPositionExpired(strategyName, security.getName(), isShort,
                excludedForceCloseBars)) return;
        String exitSignal = isShort ? "COVR" : "SELL";
        log.warn("CryptoIntradayStrategyRunner: excluded pair {}/{} has stale open position — force-closing with {}",
                strategyName, security.getName(), exitSignal);
        emitSignal(exitSignal, strategyName, security.getName(), series, series.getEndIndex(), null);
    }

    /**
     * Captures the top-of-book spread at signal time. Best-effort by design: the
     * spread is diagnostic data used to measure the true cost of a round trip, and
     * must never fail, delay or block a trading signal. One extra REST call per
     * emitted signal (a handful per 15m cycle), not per evaluated product.
     */
    private void recordSpread(IntradaySignal signal, String ticker) {
        if (!spreadLoggingEnabled) return;
        try {
            ProductBook book = productService.getProductBook(ticker);
            if (book == null) return;
            signal.setSpreadPercent(book.spreadPercent());
            signal.setBestBid(book.bestBid());
            signal.setBestAsk(book.bestAsk());
        } catch (Exception e) {
            log.warn("CryptoIntradayStrategyRunner: spread capture failed for {} — {}", ticker, e.toString());
        }
    }

    private void emitSignal(String signalType, String strategyName, String ticker,
                            BarSeries series, int index, SignalStrength strength) {
        Bar bar = series.getBar(index);
        long barStartEpoch = barStartEpoch(series, index);

        if (signalRepository.existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
                strategyName, ticker, barStartEpoch, signalType)) {
            return;
        }

        IntradaySignal signal = new IntradaySignal(
                strategyName, ticker, barStartEpoch, signalType,
                BigDecimal.valueOf(bar.getClosePrice().doubleValue())
        );
        if (strength != null) {
            signal.setSignalStrength(strength.name());
        }
        recordSpread(signal, ticker);
        signalRepository.save(signal);

        log.info("CryptoIntradayStrategyRunner: {} {} — ticker={}, bar={}, close={}, strength={}, spread={}",
                strategyName, signalType, ticker, barStartEpoch, signal.getClosePrice(),
                strength != null ? strength : "n/a",
                signal.getSpreadPercent() != null ? signal.getSpreadPercent() + "%" : "n/a");

        dispatchPaperOrder(signalType, strategyName, ticker, signal.getClosePrice(), strength);
        dispatchLiveOrder(signalType, strategyName, ticker, signal.getClosePrice(), signal, strength);
    }

    /**
     * Simulates a fill against the shared crypto paper account for long-only
     * strategies, regardless of whether live trading is enabled. SHRT/COVR
     * signals (short strategies) are skipped — paper trading mirrors what live
     * trading would actually do, and Coinbase doesn't support shorting.
     */
    private void dispatchPaperOrder(String signalType, String strategyName, String ticker,
                                    BigDecimal closePrice, SignalStrength strength) {
        if (paperTradingService == null) return;
        if (!"BUY".equals(signalType) && !"SELL".equals(signalType)) return;

        if ("BUY".equals(signalType)) {
            paperTradingService.dispatchBuy(strategyName, ticker, closePrice, strength);
        } else {
            paperTradingService.dispatchSell(strategyName, ticker, closePrice);
        }
    }

    /**
     * Routes BUY/SELL signals to Coinbase for long-only strategies.
     * SHRT and COVR signals (short strategies) are intentionally skipped.
     * Marks {@code signal.live = true} when the order is successfully filled.
     */
    private void dispatchLiveOrder(String signalType, String strategyName, String ticker,
                                   BigDecimal closePrice, IntradaySignal signal, SignalStrength strength) {
        if (liveTradingGate == null || coinbaseOrderClient == null || liveOrderRepository == null) return;
        if (!"BUY".equals(signalType) && !"SELL".equals(signalType)) return; // skip SHRT/COVR

        if ("BUY".equals(signalType)) {
            dispatchBuy(strategyName, ticker, closePrice, signal, strength);
        } else {
            dispatchSell(strategyName, ticker, signal);
        }
    }

    private void dispatchBuy(String strategyName, String ticker, BigDecimal closePrice,
                             IntradaySignal signal, SignalStrength strength) {
        BigDecimal positionEur = liveTradingGate.computePositionSizeEur(strength);
        if (!liveTradingGate.canTrade(ticker, positionEur)) return;

        OrderResponse resp = coinbaseOrderClient.placeBuyOrder(ticker, positionEur);

        LiveOrder order = new LiveOrder();
        order.setTicker(ticker);
        order.setSide("BUY");
        order.setStrategyName(strategyName);
        order.setEurAmount(positionEur);
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
