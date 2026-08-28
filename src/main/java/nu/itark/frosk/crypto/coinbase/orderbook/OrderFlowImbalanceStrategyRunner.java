package nu.itark.frosk.crypto.coinbase.orderbook;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.OrderBookMinuteSnapshot;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.OrderBookMinuteSnapshotRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.CryptoPaperTradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mechanical, already-locked implementation of §5 (Trade construction) of
 * {@code ~/itark/PREREG_ofi_1m.md} — entry when the trailing 15-minute OFI sum
 * is in the top decile of that product's trailing 30-day distribution, filled
 * one minute later at the ask; exit a fixed 15 minutes after entry, at the
 * bid; long only; no other filters.
 *
 * <p><b>Building and running this is not the same as evaluating it.</b> This
 * only produces the forward paper-trading track record the pre-registration's
 * decision rule (§9) will eventually be scored against, exactly the same
 * relationship {@code CryptoLiquiditySweepIntradayStrategy} has to
 * {@code PREREG_liquidity_sweep_15m.md}. Do not inspect this strategy's paper
 * P&amp;L, win rate, or SQN before Gate A/B in §2 are met — the same peeking
 * risk applies. It is flagged {@code preRegistrationPending} in
 * {@code DataController.PRE_REGISTRATION_PENDING_STRATEGIES} for the same
 * reason and shown with the same badge as the sweep strategy, not hidden.
 *
 * <p><b>Deliberately NOT a ta4j {@code Strategy}/{@code StrategiesMap} entry.</b>
 * §5's entry-at-ask/exit-at-bid asymmetry (the mechanism that prices in the
 * spread — see §6) has no honest expression in this codebase's existing
 * {@code TradeExecutionModel}/{@code CostModel} abstractions, which assume one
 * fill price series shared by both legs. Forcing this into a synthetic ta4j
 * bar to reuse {@code StrategiesMap} would either silently flatten that
 * asymmetry or require a workaround risky enough to compromise the numbers
 * the pre-registration cares most about. This class works directly against
 * the persisted bid/ask/OFI data instead, and reuses the existing
 * {@link IntradaySignal} ledger and {@link CryptoPaperTradingService} fill
 * simulation for everything downstream of "what price did this fill at" —
 * dashboard visibility, paper account, fee accounting all come for free and
 * stay consistent with every other strategy.
 */
@Service
@Profile("crypto")
@Slf4j
public class OrderFlowImbalanceStrategyRunner {

    public static final String STRATEGY_NAME = "OrderFlowImbalanceStrategy";

    /** §4 — the rolling window the primary (decision-rule) signal is built from. */
    private static final int OFI_WINDOW_MINUTES = 15;
    /** §5 — fixed holding period. */
    private static final int HOLD_MINUTES = 15;
    /** §5 — top decile. */
    private static final double ENTRY_PERCENTILE = 0.90;
    /** §5 — the distribution the threshold is drawn from. */
    private static final int LOOKBACK_DAYS = 30;

    /**
     * Engineering floor before a threshold is computed at all — NOT the
     * pre-registration's Gate B (§2, 20 days / SE-based). This just avoids a
     * degenerate percentile off a handful of points; it has no bearing on
     * whether the eventual result is trustworthy enough to score against §9.
     */
    private static final int MIN_ROWS_FOR_THRESHOLD = 3 * 24 * 60;

    @Value("${coinbase.l2.capture.products:BTC-EUR,ETH-EUR}")
    private String productsRaw;

    @Autowired
    private OrderBookMinuteSnapshotRepository snapshotRepository;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private IntradaySignalRepository intradaySignalRepository;

    @Autowired
    private CryptoPaperTradingService paperTradingService;

    private List<String> products;
    private final Map<String, BigDecimal> dailyThreshold = new ConcurrentHashMap<>();
    /** True once a threshold crossing was detected last minute, waiting for this minute's ask fill (§5). */
    private final Map<String, Boolean> pendingEntry = new ConcurrentHashMap<>();

    private List<String> products() {
        if (products == null) {
            products = Arrays.stream(productsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return products;
    }

    /** Recomputes each product's entry threshold once a day, fixed for that day per §5. */
    @Scheduled(cron = "0 10 0 * * *")
    public void recomputeDailyThresholds() {
        for (String product : products()) {
            recomputeThreshold(product);
        }
    }

    private void recomputeThreshold(String product) {
        Security security = securityRepository.findByName(product);
        if (security == null) return;

        long cutoff = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS).getEpochSecond();
        List<OrderBookMinuteSnapshot> rows = snapshotRepository
                .findBySecurityIdAndMinuteTimestampGreaterThanEqualOrderByMinuteTimestampAsc(security.getId(), cutoff);

        if (rows.size() < MIN_ROWS_FOR_THRESHOLD) {
            log.info("OrderFlowImbalanceStrategyRunner: {} has {} rows, need {} — no threshold yet",
                    product, rows.size(), MIN_ROWS_FOR_THRESHOLD);
            dailyThreshold.remove(product);
            return;
        }

        List<BigDecimal> ofi15mSeries = rollingOfi15m(rows);
        if (ofi15mSeries.isEmpty()) {
            dailyThreshold.remove(product);
            return;
        }
        List<BigDecimal> sorted = new ArrayList<>(ofi15mSeries);
        sorted.sort(Comparator.naturalOrder());
        int idx = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(ENTRY_PERCENTILE * sorted.size()) - 1));
        BigDecimal threshold = sorted.get(idx);
        dailyThreshold.put(product, threshold);
        log.info("OrderFlowImbalanceStrategyRunner: {} threshold={} from {} fifteen-minute windows",
                product, threshold, sorted.size());
    }

    /**
     * Sum of {@code ofiSum} over each consecutive, gap-free 15-minute window
     * in {@code rows} (ascending order). A window spanning a capture gap is
     * not a real 15-minute reading and is skipped rather than approximated —
     * same honesty principle as {@code OrderBookCaptureService}'s "an absent
     * minute is absent, not zero".
     */
    /** Package-private for direct unit testing (see TestJOrderFlowImbalanceStrategyRunner). */
    List<BigDecimal> rollingOfi15m(List<OrderBookMinuteSnapshot> rows) {
        List<BigDecimal> result = new ArrayList<>();
        Deque<OrderBookMinuteSnapshot> window = new ArrayDeque<>();
        BigDecimal windowSum = BigDecimal.ZERO;
        for (OrderBookMinuteSnapshot row : rows) {
            if (!window.isEmpty() && row.getMinuteTimestamp() != window.peekLast().getMinuteTimestamp() + 60) {
                window.clear();
                windowSum = BigDecimal.ZERO;
            }
            window.addLast(row);
            windowSum = windowSum.add(row.getOfiSum());
            if (window.size() > OFI_WINDOW_MINUTES) {
                windowSum = windowSum.subtract(window.removeFirst().getOfiSum());
            }
            if (window.size() == OFI_WINDOW_MINUTES) {
                result.add(windowSum);
            }
        }
        return result;
    }

    /**
     * Fires shortly after {@code OrderBookCaptureService.captureMinute()} so
     * the just-completed minute's row is already committed.
     */
    @Scheduled(cron = "5 * * * * *")
    public void evaluateMinute() {
        for (String product : products()) {
            try {
                evaluateProduct(product);
            } catch (Exception e) {
                log.warn("OrderFlowImbalanceStrategyRunner: evaluation failed for {} — {}", product, e.toString());
            }
        }
    }

    private void evaluateProduct(String product) {
        Security security = securityRepository.findByName(product);
        if (security == null) return;

        Optional<IntradaySignal> latestBuy = intradaySignalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(STRATEGY_NAME, product, "BUY");
        Optional<IntradaySignal> latestSell = intradaySignalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(STRATEGY_NAME, product, "SELL");
        boolean isOpen = latestBuy.isPresent()
                && (latestSell.isEmpty() || latestBuy.get().getSignalTimestamp() > latestSell.get().getSignalTimestamp());

        OrderBookMinuteSnapshot latestSnapshot = snapshotRepository.findTopBySecurityIdOrderByMinuteTimestampDesc(security.getId());
        if (latestSnapshot == null) return;

        if (isOpen) {
            long heldSeconds = latestSnapshot.getMinuteTimestamp() - latestBuy.get().getSignalTimestamp();
            if (heldSeconds >= HOLD_MINUTES * 60L) {
                exit(product, latestSnapshot);
            }
            return;
        }

        if (Boolean.TRUE.equals(pendingEntry.get(product))) {
            enter(product, latestSnapshot);
            pendingEntry.put(product, false);
            return;
        }

        BigDecimal threshold = dailyThreshold.get(product);
        if (threshold == null) return;

        long cutoff = latestSnapshot.getMinuteTimestamp() - (OFI_WINDOW_MINUTES - 1) * 60L;
        List<OrderBookMinuteSnapshot> last15 = snapshotRepository
                .findBySecurityIdAndMinuteTimestampBetweenOrderByMinuteTimestampAsc(
                        security.getId(), cutoff, latestSnapshot.getMinuteTimestamp());
        List<BigDecimal> windowSums = rollingOfi15m(last15);
        if (windowSums.isEmpty()) return; // not 15 consecutive gap-free minutes yet

        BigDecimal currentOfi15m = windowSums.get(windowSums.size() - 1);
        if (currentOfi15m.compareTo(threshold) > 0) {
            log.info("OrderFlowImbalanceStrategyRunner: {} OFI_15m={} crossed threshold={} — entering next minute at ask",
                    product, currentOfi15m, threshold);
            pendingEntry.put(product, true);
        }
    }

    private void enter(String product, OrderBookMinuteSnapshot snapshot) {
        if (snapshot.getBestAskPrice() == null) return;
        if (intradaySignalRepository.existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
                STRATEGY_NAME, product, snapshot.getMinuteTimestamp(), "BUY")) {
            return;
        }
        IntradaySignal signal = new IntradaySignal(STRATEGY_NAME, product, snapshot.getMinuteTimestamp(),
                "BUY", snapshot.getBestAskPrice());
        intradaySignalRepository.save(signal);
        log.info("OrderFlowImbalanceStrategyRunner: BUY {} @ {} (ask)", product, snapshot.getBestAskPrice());
        paperTradingService.dispatchBuy(STRATEGY_NAME, product, snapshot.getBestAskPrice());
    }

    private void exit(String product, OrderBookMinuteSnapshot snapshot) {
        if (snapshot.getBestBidPrice() == null) return;
        if (intradaySignalRepository.existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
                STRATEGY_NAME, product, snapshot.getMinuteTimestamp(), "SELL")) {
            return;
        }
        IntradaySignal signal = new IntradaySignal(STRATEGY_NAME, product, snapshot.getMinuteTimestamp(),
                "SELL", snapshot.getBestBidPrice());
        intradaySignalRepository.save(signal);
        log.info("OrderFlowImbalanceStrategyRunner: SELL {} @ {} (bid)", product, snapshot.getBestBidPrice());
        paperTradingService.dispatchSell(STRATEGY_NAME, product, snapshot.getBestBidPrice());
    }
}
