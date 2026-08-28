package nu.itark.frosk.crypto.coinbase.orderbook;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.OrderBookMinuteSnapshot;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.OrderBookMinuteSnapshotRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Flushes {@link CoinbaseLevel2WebSocketClient}'s per-product {@link OrderBookState}
 * once a minute into {@code order_book_minute_snapshot} — the data gate for
 * {@code ~/itark/PREREG_ofi_1m.md}.
 *
 * <p>A minute with no persisted row (WebSocket disconnected, app down, etc.)
 * is simply absent rather than written as an explicit "gap" row — the
 * pre-registration's {@code pctMinutesCaptured} bookkeeping (§2) is meant to
 * be computed at analysis time from the density of rows actually present,
 * not tracked separately here.
 */
@Service
@Profile("crypto")
@Slf4j
public class OrderBookCaptureService {

    @Autowired
    private CoinbaseLevel2WebSocketClient wsClient;

    @Autowired
    private OrderBookMinuteSnapshotRepository snapshotRepository;

    @Autowired
    private SecurityRepository securityRepository;

    /**
     * Retention floor comfortably above PREREG_ofi_1m.md §2's Gate B (20 days),
     * so re-running the analysis after the gate is met doesn't need a fresh capture.
     */
    @Value("${coinbase.l2.capture.retention.days:35}")
    private int retentionDays;

    /** Fires at the top of every minute — flushes the minute that JUST ended. */
    @Scheduled(cron = "0 * * * * *")
    public void captureMinute() {
        if (wsClient.getTrackedProducts().isEmpty()) {
            return; // capture disabled (coinbase.l2.capture.enabled=false)
        }
        long minuteTimestamp = currentMinuteFloor() - 60;

        for (String product : wsClient.getTrackedProducts()) {
            OrderBookState state = wsClient.getState(product);
            if (state == null) continue;

            OrderBookState.MinuteFlush flush = state.flushMinute();
            if (flush.bidPrice() == null || flush.askPrice() == null) {
                log.debug("OrderBookCaptureService: {} book not populated yet — skipping this minute", product);
                continue;
            }

            Security security = securityRepository.findByName(product);
            if (security == null) {
                log.warn("OrderBookCaptureService: no Security row for {} — run dataset/security setup first", product);
                continue;
            }
            if (snapshotRepository.existsBySecurityIdAndMinuteTimestamp(security.getId(), minuteTimestamp)) {
                continue; // idempotent guard, same convention as IntradayBar
            }

            snapshotRepository.save(new OrderBookMinuteSnapshot(
                    security.getId(), minuteTimestamp,
                    flush.bidPrice(), flush.bidSize(), flush.askPrice(), flush.askSize(),
                    flush.ofiSum(), flush.eventCount()
            ));
        }
    }

    /** Once daily — mirrors the retention-pruning convention already used for intraday_bar. */
    @Scheduled(cron = "0 40 3 * * *")
    public void pruneOld() {
        long cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS).getEpochSecond();
        int deleted = snapshotRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("OrderBookCaptureService: pruned {} rows older than {} days", deleted, retentionDays);
        }
    }

    private long currentMinuteFloor() {
        long now = Instant.now().getEpochSecond();
        return now - (now % 60);
    }
}
