package nu.itark.frosk.crypto.livetrading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.broker.ProtectiveStopClient;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import nu.itark.frosk.strategies.SignalStrength;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Places real broker orders for intraday signals and records them in
 * {@code live_order}.
 *
 * <p>Shared by the Coinbase runner path and the Kraken
 * {@code LiveKrakenOrderDispatcher}. Every entry passes
 * {@link LiveTradingGate#canTrade}, so the global
 * {@code crypto.live.trading.enabled} switch stays the master kill switch.
 *
 * <h3>Broker statuses</h3>
 * <ul>
 *   <li>{@code FILLED} — recorded with the broker's fill size and price.</li>
 *   <li>{@code PENDING} — Coinbase's "accepted"; treated as filled exactly as before.</li>
 *   <li>{@code UNCONFIRMED} — Kraken placed the order but no fill is confirmed yet.
 *       Saved as {@code PENDING} WITHOUT a quantity and left to
 *       {@code KrakenFuturesLiveOrderReconciler}; never booked as filled on a guess,
 *       because the recorded quantity is exactly what the exit later closes.</li>
 * </ul>
 */
@Service
@Profile({"crypto", "kraken-futures"})
@RequiredArgsConstructor
@Slf4j
public class LiveOrderExecutor {

    /** Kraken order status for a placed-but-unconfirmed order (KrakenFuturesOrderClient.STATUS_UNCONFIRMED). */
    static final String UNCONFIRMED = "UNCONFIRMED";

    /** Entry statuses meaning a position is, or may be, open at the broker. */
    public static final Set<String> OPEN_ENTRY_STATUSES = Set.of("FILLED", "PENDING", "CLOSING", "UNRESOLVED");

    private final LiveTradingGate liveTradingGate;
    private final BrokerOrderClient brokerOrderClient;
    private final LiveOrderRepository liveOrderRepository;
    private final IntradaySignalRepository signalRepository;

    /**
     * Distance of the exchange-side protective stop from the entry fill, percent.
     * 0 disables it. Defaults to the VWAP strategy's own 2.7 % stop, so the resting
     * stop and the strategy's bar-close stop sit at the same level — the resting one
     * simply gets there first when a single bar blows through it.
     */
    @Value("${kraken.futures.protective.stop.pct:2.7}")
    private BigDecimal protectiveStopPct = new BigDecimal("2.7");

    /**
     * Routes a signal to the broker. Marks {@code signal.live = true} when the
     * order is filled.
     *
     * @param signal        the persisted signal, or null for synthetic exits
     * @param executionMode tag written to {@code live_order.execution_mode}; null on Coinbase
     */
    public void dispatch(String signalType, String strategyName, String ticker, BigDecimal closePrice,
                         IntradaySignal signal, SignalStrength strength, String executionMode) {
        boolean isShort = "SHRT".equals(signalType) || "COVR".equals(signalType);

        // Coinbase does not support short selling — skip SHRT/COVR on non-short brokers
        if (isShort && !brokerOrderClient.supportsShort()) return;

        if ("BUY".equals(signalType) || "SHRT".equals(signalType)) {
            dispatchEntry(signalType, strategyName, ticker, closePrice, signal, strength, executionMode);
        } else if ("SELL".equals(signalType) || "COVR".equals(signalType)) {
            dispatchExit(signalType, strategyName, ticker, signal, executionMode);
        }
    }

    private void dispatchEntry(String signalType, String strategyName, String ticker, BigDecimal closePrice,
                               IntradaySignal signal, SignalStrength strength, String executionMode) {
        String side = "SHRT".equals(signalType) ? "SHRT" : "BUY";
        String opposite = "SHRT".equals(side) ? "BUY" : "SHRT";

        // Kraken nets positions per symbol. A long from one strategy and a short
        // from another on the same symbol cancel out on the exchange, after which
        // each strategy's reduce-only close is rejected and neither can exit.
        List<LiveOrder> opposing = liveOrderRepository.findByTickerAndSideInAndStatusIn(
                ticker, List.of(opposite), OPEN_ENTRY_STATUSES);
        if (!opposing.isEmpty()) {
            log.warn("LIVE ORDER{} REFUSED: {} {} {} — {} already holds an open {} on {}; opposite positions "
                            + "would net out on the exchange and become impossible to close per strategy",
                    tag(executionMode), strategyName, side, ticker, opposing.get(0).getStrategyName(), opposite, ticker);
            return;
        }

        BigDecimal positionSize = liveTradingGate.computePositionSizeEur(strength);
        if (!liveTradingGate.canTrade(ticker, positionSize)) {
            if (executionMode != null) {
                // A strategy promoted to SHADOW/LIVE whose entry the global gate blocks
                // is otherwise silent (canTrade logs the master-switch case at DEBUG).
                log.warn("LIVE ORDER [{}] BLOCKED by LiveTradingGate: {} {} {} — check crypto.live.trading.enabled "
                        + "and account balance", executionMode, strategyName, signalType, ticker);
            }
            return;
        }

        OrderResponse resp = "SHRT".equals(side)
                ? brokerOrderClient.placeShortEntry(ticker, positionSize)
                : brokerOrderClient.placeLongEntry(ticker, positionSize);

        LiveOrder order = new LiveOrder();
        order.setTicker(ticker);
        order.setSide(side);
        order.setStrategyName(strategyName);
        order.setEurAmount(positionSize);
        order.setCoinbaseOrderId(resp.getOrderId());
        order.setClientOrderId(resp.getClientOrderId());
        order.setExecutionMode(executionMode);

        if ("PENDING".equals(resp.getStatus()) || "FILLED".equals(resp.getStatus())) {
            order.setStatus("FILLED");
            order.setFilledPrice(resp.getAverageFilledPrice() != null ? resp.getAverageFilledPrice() : closePrice);
            order.setFilledQuantity(resp.getFilledSize());
            order.setFilledAt(LocalDateTime.now());
            markLive(signal);
            log.info("LIVE ORDER{}: {} {} {} @ {} (orderId={})",
                    tag(executionMode), side, ticker, resp.getFilledSize(), resp.getAverageFilledPrice(), resp.getOrderId());
        } else if (UNCONFIRMED.equals(resp.getStatus())) {
            order.setStatus("PENDING");
            log.warn("LIVE ORDER{}: {} {} placed (orderId={}) — fill unconfirmed, awaiting reconciler",
                    tag(executionMode), side, ticker, resp.getOrderId());
        } else {
            order.setStatus("FAILED");
            order.setErrorMessage(truncate(resp.getErrorMessage()));
            log.warn("LIVE ORDER FAILED{}: {} {} — {}", tag(executionMode), side, ticker, resp.getErrorMessage());
        }
        liveOrderRepository.save(order);

        // After the save, so the row has an id to attach the stop's order id to.
        if ("FILLED".equals(order.getStatus())) {
            attachProtectiveStop(order);
        }
    }

    private void dispatchExit(String signalType, String strategyName, String ticker,
                              IntradaySignal signal, String executionMode) {
        String entrySide = "COVR".equals(signalType) ? "SHRT" : "BUY";
        Optional<LiveOrder> openEntry = liveOrderRepository
                .findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(
                        ticker, strategyName, entrySide, "FILLED");

        if (openEntry.isEmpty()) {
            Optional<LiveOrder> unconfirmed = liveOrderRepository
                    .findTopByTickerAndStrategyNameAndSideAndStatusInOrderByCreatedAtDesc(
                            ticker, strategyName, entrySide, List.of("PENDING", "UNRESOLVED"));
            if (unconfirmed.isPresent()) {
                // The reconciler checks for an exit signal after confirming the entry
                // and closes it then — the position is not dropped.
                log.warn("LIVE ORDER{}: {} for {}/{} arrived while its entry {} is still {} — the reconciler "
                                + "will close it once the fill is confirmed",
                        tag(executionMode), signalType, strategyName, ticker,
                        unconfirmed.get().getCoinbaseOrderId(), unconfirmed.get().getStatus());
            } else {
                log.debug("LiveOrderExecutor: {} signal for {} / {} but no open {} — skip",
                        signalType, ticker, strategyName, entrySide);
            }
            return;
        }
        closeEntry(openEntry.get(), signal, executionMode);
    }

    /**
     * Places the closing order for a filled entry and settles it. Public so the
     * Kraken reconciler can close an entry whose exit signal arrived before its
     * own fill was confirmed.
     */
    public void closeEntry(LiveOrder entryOrder, IntradaySignal signal, String executionMode) {
        String ticker = entryOrder.getTicker();
        boolean isCover = "SHRT".equals(entryOrder.getSide());
        String exitSide = isCover ? "COVR" : "SELL";

        if (entryOrder.getFilledQuantity() == null
                || entryOrder.getFilledQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("LiveOrderExecutor: open {} {} (id={}) has no filled quantity — cannot close, check the broker",
                    entryOrder.getSide(), ticker, entryOrder.getId());
            return;
        }

        // Retire the resting stop first: closing the position while its stop is still
        // live leaves an orphan order on the exchange.
        cancelProtectiveStop(entryOrder);

        OrderResponse resp = isCover
                ? brokerOrderClient.placeShortExit(ticker, entryOrder.getFilledQuantity())
                : brokerOrderClient.placeLongExit(ticker, entryOrder.getFilledQuantity());

        // The exit carries the mode its ENTRY was opened under, not the strategy's
        // current mode — after a rollback a LIVE entry is still closed as LIVE.
        String exitMode = entryOrder.getExecutionMode() != null ? entryOrder.getExecutionMode() : executionMode;

        LiveOrder exitOrder = new LiveOrder();
        exitOrder.setTicker(ticker);
        exitOrder.setSide(exitSide);
        exitOrder.setStrategyName(entryOrder.getStrategyName());
        exitOrder.setEntryOrderId(entryOrder.getId());
        exitOrder.setCoinbaseOrderId(resp.getOrderId());
        exitOrder.setClientOrderId(resp.getClientOrderId());
        exitOrder.setExecutionMode(exitMode);
        // This path is always the strategy's own SELL/COVR signal — a stop-triggered
        // close never reaches here, it is detected and settled separately by
        // KrakenFuturesLiveOrderReconciler.checkProtectiveStop(), which overwrites
        // this with PROTECTIVE_STOP on its own exit row.
        exitOrder.setCloseReason("SIGNAL");

        if ("PENDING".equals(resp.getStatus()) || "FILLED".equals(resp.getStatus())) {
            markLive(signal);
            // Coinbase PENDING may report filled size 0 at response time; it has always
            // meant "whole quantity", so only a FILLED status's size is taken literally.
            BigDecimal filledSize = "FILLED".equals(resp.getStatus()) ? resp.getFilledSize() : null;
            settleExit(entryOrder, exitOrder, filledSize, resp.getAverageFilledPrice());
        } else if (UNCONFIRMED.equals(resp.getStatus())) {
            exitOrder.setStatus("PENDING");
            liveOrderRepository.save(exitOrder);
            // CLOSING keeps a second exit signal from sending another close while this one is unconfirmed.
            entryOrder.setStatus("CLOSING");
            liveOrderRepository.save(entryOrder);
            log.warn("LIVE ORDER{}: {} {} placed (orderId={}) — fill unconfirmed, entry {} CLOSING until the "
                    + "reconciler confirms", tag(exitMode), exitSide, ticker, resp.getOrderId(), entryOrder.getId());
        } else {
            exitOrder.setStatus("FAILED");
            exitOrder.setFilledQuantity(entryOrder.getFilledQuantity());
            exitOrder.setErrorMessage(truncate(resp.getErrorMessage()));
            liveOrderRepository.save(exitOrder);
            log.error("LIVE ORDER FAILED{}: {} {} — {} — position {} still OPEN", tag(exitMode), exitSide, ticker,
                    resp.getErrorMessage(), entryOrder.getId());
        }
    }

    /**
     * Records a filled exit against its entry: PnL on the quantity that actually
     * closed, and the entry CLOSED — or, on a partial close, left FILLED with the
     * remaining quantity so it can still be closed. Public for the reconciler.
     *
     * @param filledSize broker-reported executed size; null on Coinbase, where the
     *                   whole entry quantity is assumed closed (previous behaviour)
     */
    public void settleExit(LiveOrder entryOrder, LiveOrder exitOrder, BigDecimal filledSize, BigDecimal exitPrice) {
        BigDecimal entryQty = entryOrder.getFilledQuantity();
        BigDecimal closedQty = filledSize != null ? filledSize.min(entryQty) : entryQty;
        boolean isCover = "SHRT".equals(entryOrder.getSide());

        exitOrder.setStatus("FILLED");
        exitOrder.setFilledQuantity(closedQty);
        exitOrder.setFilledPrice(exitPrice);
        exitOrder.setFilledAt(LocalDateTime.now());
        if (exitPrice != null && entryOrder.getFilledPrice() != null) {
            BigDecimal move = isCover
                    ? entryOrder.getFilledPrice().subtract(exitPrice)   // short profits when price falls
                    : exitPrice.subtract(entryOrder.getFilledPrice());
            exitOrder.setRealizedPnlEur(move.multiply(closedQty));
        }
        liveOrderRepository.save(exitOrder);

        BigDecimal remaining = entryQty.subtract(closedQty);
        if (remaining.signum() > 0) {
            entryOrder.setFilledQuantity(remaining);
            entryOrder.setStatus("FILLED");
            log.error("LIVE ORDER: PARTIAL close of {} {} (entry {}) — {} closed, {} still OPEN on the broker",
                    entryOrder.getSide(), entryOrder.getTicker(), entryOrder.getId(),
                    closedQty.toPlainString(), remaining.toPlainString());
        } else {
            entryOrder.setStatus("CLOSED");
        }
        liveOrderRepository.save(entryOrder);

        log.info("LIVE ORDER{}: {} {} {} @ {} (pnl={}, orderId={})",
                tag(exitOrder.getExecutionMode()), exitOrder.getSide(), exitOrder.getTicker(),
                closedQty.toPlainString(), exitPrice, exitOrder.getRealizedPnlEur(), exitOrder.getCoinbaseOrderId());
    }

    /**
     * Rests a reduce-only stop on the exchange for a freshly filled entry, when the
     * broker supports it. Best-effort: a failure here leaves the position protected
     * only by the strategy's bar-close stop, which is the behaviour that existed
     * before — so it is logged loudly but never unwinds the entry.
     */
    public void attachProtectiveStop(LiveOrder entry) {
        if (!(brokerOrderClient instanceof ProtectiveStopClient stopClient)) return;
        if (protectiveStopPct == null || protectiveStopPct.signum() <= 0) return;
        if (entry.getFilledQuantity() == null || entry.getFilledQuantity().signum() <= 0
                || entry.getFilledPrice() == null || entry.getFilledPrice().signum() <= 0) {
            log.error("LiveOrderExecutor: cannot place protective stop for entry {} — no fill price/quantity",
                    entry.getId());
            return;
        }
        boolean isLong = "BUY".equals(entry.getSide());
        OrderResponse stop = stopClient.placeProtectiveStop(entry.getTicker(), isLong,
                entry.getFilledQuantity(), entry.getFilledPrice(), protectiveStopPct);

        if ("PLACED".equals(stop.getStatus()) && stop.getOrderId() != null) {
            entry.setProtectiveStopOrderId(stop.getOrderId());
            liveOrderRepository.save(entry);
            log.info("LIVE ORDER{}: protective stop {} attached to {} {} (entry {}, {} % from {})",
                    tag(entry.getExecutionMode()), stop.getOrderId(), entry.getSide(), entry.getTicker(),
                    entry.getId(), protectiveStopPct, entry.getFilledPrice());
        } else {
            log.error("LIVE ORDER{}: NO protective stop on {} {} (entry {}) — {}. The position is only protected "
                            + "by the strategy's 15m bar-close stop.",
                    tag(entry.getExecutionMode()), entry.getSide(), entry.getTicker(), entry.getId(),
                    stop.getErrorMessage());
        }
    }

    /** Cancels an entry's resting stop and clears it from the row. */
    private void cancelProtectiveStop(LiveOrder entry) {
        if (entry.getProtectiveStopOrderId() == null) return;
        if (!(brokerOrderClient instanceof ProtectiveStopClient stopClient)) return;
        String stopId = entry.getProtectiveStopOrderId();
        boolean gone = stopClient.cancelOrder(stopId);
        if (gone) {
            entry.setProtectiveStopOrderId(null);
            liveOrderRepository.save(entry);
        }
        // A failed cancel is logged by the client; the id is kept so the reconciler
        // still notices if that stop turns out to have filled.
    }

    private void markLive(IntradaySignal signal) {
        if (signal == null) return;
        signal.setLive(true);
        signalRepository.save(signal);
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 255);
    }

    private static String tag(String executionMode) {
        return executionMode == null ? "" : " [" + executionMode + "]";
    }
}
