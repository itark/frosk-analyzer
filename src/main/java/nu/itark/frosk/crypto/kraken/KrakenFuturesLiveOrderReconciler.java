package nu.itark.frosk.crypto.kraken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.livetrading.LiveOrderExecutor;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Resolves Kraken orders that were placed but whose fill was not confirmed at
 * send time ({@code live_order.status = PENDING}), from {@code GET /fills}.
 *
 * <p>For a confirmed <b>entry</b> it records the real size and price — the
 * quantity its exit will close — and, if the strategy's exit signal already
 * fired while the entry was unconfirmed, closes it immediately (the runner
 * emits each exit signal once, so nothing else would).
 *
 * <p>For a confirmed <b>exit</b> it settles PnL against the linked entry and
 * closes it.
 *
 * <p>An order with no fill after {@code kraken.futures.reconcile.give.up.minutes}
 * becomes {@code UNRESOLVED} with an ERROR log: it may be open on Kraken, so it
 * keeps counting towards exposure and blocks opposite-direction entries, but it
 * needs a human to check the exchange.
 */
@Component
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenFuturesLiveOrderReconciler {

    private final LiveOrderRepository liveOrderRepository;
    private final KrakenFuturesOrderClient orderClient;
    private final LiveOrderExecutor liveOrderExecutor;
    private final IntradaySignalRepository signalRepository;

    @Value("${kraken.futures.reconcile.give.up.minutes:30}")
    private long giveUpMinutes = 30;

    @Scheduled(fixedDelayString = "${kraken.futures.reconcile.interval.ms:60000}", initialDelay = 60000)
    public void reconcile() {
        List<LiveOrder> pending = liveOrderRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        for (LiveOrder order : pending) {
            try {
                reconcile(order);
            } catch (Exception e) {
                log.error("KrakenFuturesLiveOrderReconciler: failed on order {} ({})", order.getId(),
                        order.getCoinbaseOrderId(), e);
            }
        }
        for (LiveOrder entry : liveOrderRepository.findByStatusAndProtectiveStopOrderIdIsNotNull("FILLED")) {
            try {
                checkProtectiveStop(entry);
            } catch (Exception e) {
                log.error("KrakenFuturesLiveOrderReconciler: failed checking stop {} on entry {}",
                        entry.getProtectiveStopOrderId(), entry.getId(), e);
            }
        }
    }

    /**
     * Detects a protective stop that has fired. The exchange closes the position on
     * its own, so without this the app would keep treating it as open: its exposure
     * would be double-counted, its exit signal would hit an already-flat position,
     * and the realized loss would never be booked.
     */
    void checkProtectiveStop(LiveOrder entry) {
        KrakenFuturesOrderClient.Fill fill = orderClient.findFill(entry.getProtectiveStopOrderId());
        if (fill == null) return;

        log.warn("KrakenFuturesLiveOrderReconciler: PROTECTIVE STOP FIRED on {} {} (entry {}) — closed {} @ {}",
                entry.getSide(), entry.getTicker(), entry.getId(), fill.size().toPlainString(), fill.averagePrice());

        LiveOrder exit = new LiveOrder();
        exit.setTicker(entry.getTicker());
        exit.setSide("SHRT".equals(entry.getSide()) ? "COVR" : "SELL");
        exit.setStrategyName(entry.getStrategyName());
        exit.setEntryOrderId(entry.getId());
        exit.setCoinbaseOrderId(entry.getProtectiveStopOrderId());
        exit.setExecutionMode(entry.getExecutionMode());
        exit.setCloseReason("PROTECTIVE_STOP");

        // The stop is gone once filled; clear it before settling so the closed entry
        // is not picked up again on the next pass.
        entry.setProtectiveStopOrderId(null);
        liveOrderExecutor.settleExit(entry, exit, fill.size(), fill.averagePrice());
    }

    void reconcile(LiveOrder order) {
        if (order.getCoinbaseOrderId() == null) {
            giveUp(order, "no Kraken order id recorded");
            return;
        }
        KrakenFuturesOrderClient.Fill fill = orderClient.findFill(order.getCoinbaseOrderId());
        if (fill == null) {
            if (Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes() >= giveUpMinutes) {
                giveUp(order, "no fill in /fills after " + giveUpMinutes + " min");
            }
            return;
        }

        boolean isEntry = "BUY".equals(order.getSide()) || "SHRT".equals(order.getSide());
        if (isEntry) {
            confirmEntry(order, fill);
        } else {
            confirmExit(order, fill);
        }
    }

    private void confirmEntry(LiveOrder entry, KrakenFuturesOrderClient.Fill fill) {
        entry.setStatus("FILLED");
        entry.setFilledQuantity(fill.size());
        entry.setFilledPrice(fill.averagePrice());
        entry.setFilledAt(LocalDateTime.now());
        liveOrderRepository.save(entry);
        log.warn("KrakenFuturesLiveOrderReconciler: entry {} {} {} CONFIRMED late — {} @ {}",
                entry.getId(), entry.getSide(), entry.getTicker(), fill.size().toPlainString(), fill.averagePrice());

        if (exitSignalledSinceEntry(entry)) {
            // Closing right away — no point resting a stop only to cancel it.
            log.warn("KrakenFuturesLiveOrderReconciler: exit signal for {}/{} already fired — closing entry {} now",
                    entry.getStrategyName(), entry.getTicker(), entry.getId());
            liveOrderExecutor.closeEntry(entry, null, entry.getExecutionMode());
        } else {
            // A late-confirmed entry is unprotected until now: give it its stop.
            liveOrderExecutor.attachProtectiveStop(entry);
        }
    }

    private void confirmExit(LiveOrder exit, KrakenFuturesOrderClient.Fill fill) {
        Optional<LiveOrder> entry = exit.getEntryOrderId() != null
                ? liveOrderRepository.findById(exit.getEntryOrderId())
                : Optional.empty();
        if (entry.isEmpty() || entry.get().getFilledQuantity() == null) {
            // Fill recorded, but there is nothing to settle it against.
            exit.setStatus("FILLED");
            exit.setFilledQuantity(fill.size());
            exit.setFilledPrice(fill.averagePrice());
            exit.setFilledAt(LocalDateTime.now());
            liveOrderRepository.save(exit);
            log.error("KrakenFuturesLiveOrderReconciler: exit {} confirmed but its entry {} is missing — PnL not "
                    + "settled, check manually", exit.getId(), exit.getEntryOrderId());
            return;
        }
        liveOrderExecutor.settleExit(entry.get(), exit, fill.size(), fill.averagePrice());
        log.warn("KrakenFuturesLiveOrderReconciler: exit {} for entry {} CONFIRMED late", exit.getId(), entry.get().getId());
    }

    /**
     * Whether the strategy's latest exit signal for this ticker is newer than its
     * latest entry signal — the runner's own definition of "the position has been
     * closed" (see CryptoIntradayStrategyRunner.hasRealWorldOpenPosition).
     */
    boolean exitSignalledSinceEntry(LiveOrder entry) {
        boolean isShort = "SHRT".equals(entry.getSide());
        String entryType = isShort ? "SHRT" : "BUY";
        String exitType = isShort ? "COVR" : "SELL";
        Optional<IntradaySignal> lastEntry = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(
                        entry.getStrategyName(), entry.getTicker(), entryType);
        Optional<IntradaySignal> lastExit = signalRepository
                .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(
                        entry.getStrategyName(), entry.getTicker(), exitType);
        return lastEntry.isPresent() && lastExit.isPresent()
                && lastExit.get().getSignalTimestamp() > lastEntry.get().getSignalTimestamp();
    }

    private void giveUp(LiveOrder order, String reason) {
        order.setStatus("UNRESOLVED");
        order.setErrorMessage(reason);
        liveOrderRepository.save(order);
        log.error("KrakenFuturesLiveOrderReconciler: order {} ({} {} {}, Kraken id {}) UNRESOLVED — {}. It may be "
                        + "OPEN on Kraken: check the exchange and settle manually.",
                order.getId(), order.getStrategyName(), order.getSide(), order.getTicker(),
                order.getCoinbaseOrderId(), reason);
    }
}
