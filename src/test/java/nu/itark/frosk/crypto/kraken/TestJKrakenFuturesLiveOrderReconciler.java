package nu.itark.frosk.crypto.kraken;

import nu.itark.frosk.crypto.livetrading.LiveOrderExecutor;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bug 3: late fill confirmation for orders placed without a confirmed fill. */
public class TestJKrakenFuturesLiveOrderReconciler {

    private static final String STRATEGY = "CryptoShortIntradayStrategy";
    private static final String TICKER = "PF_XBTUSD";

    private LiveOrderRepository repo;
    private KrakenFuturesOrderClient client;
    private LiveOrderExecutor executor;
    private IntradaySignalRepository signals;
    private KrakenFuturesLiveOrderReconciler reconciler;

    @BeforeEach
    void setUp() {
        repo = mock(LiveOrderRepository.class);
        client = mock(KrakenFuturesOrderClient.class);
        executor = mock(LiveOrderExecutor.class);
        signals = mock(IntradaySignalRepository.class);
        reconciler = new KrakenFuturesLiveOrderReconciler(repo, client, executor, signals);
    }

    private static LiveOrder pending(String side, int minutesOld) {
        LiveOrder o = new LiveOrder();
        o.setId(1L);
        o.setTicker(TICKER);
        o.setStrategyName(STRATEGY);
        o.setSide(side);
        o.setStatus("PENDING");
        o.setCoinbaseOrderId("ord-1");
        o.setCreatedAt(LocalDateTime.now().minusMinutes(minutesOld));
        return o;
    }

    private void givenSignals(long entryTs, Long exitTs) {
        when(signals.findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(STRATEGY, TICKER, "SHRT"))
                .thenReturn(Optional.of(new IntradaySignal(STRATEGY, TICKER, entryTs, "SHRT", BigDecimal.ONE)));
        when(signals.findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(STRATEGY, TICKER, "COVR"))
                .thenReturn(exitTs == null ? Optional.empty()
                        : Optional.of(new IntradaySignal(STRATEGY, TICKER, exitTs, "COVR", BigDecimal.ONE)));
    }

    @Test
    void pendingEntry_confirmedFromFills_recordsRealQuantity() {
        LiveOrder entry = pending("SHRT", 2);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(entry));
        when(client.findFill("ord-1")).thenReturn(
                new KrakenFuturesOrderClient.Fill(new BigDecimal("0.0029"), new BigDecimal("85650")));
        givenSignals(1000, null);

        reconciler.reconcile();

        assertEquals("FILLED", entry.getStatus());
        assertEquals(new BigDecimal("0.0029"), entry.getFilledQuantity());
        assertEquals(new BigDecimal("85650"), entry.getFilledPrice());
        verify(executor, never()).closeEntry(any(), any(), any());
    }

    @Test
    void pendingEntry_whoseExitAlreadyFired_isClosedOnConfirmation() {
        LiveOrder entry = pending("SHRT", 20);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(entry));
        when(client.findFill("ord-1")).thenReturn(
                new KrakenFuturesOrderClient.Fill(new BigDecimal("0.0029"), new BigDecimal("85650")));
        givenSignals(1000, 1900L); // COVR after the SHRT

        reconciler.reconcile();

        verify(executor).closeEntry(eq(entry), eq(null), any());
    }

    @Test
    void pendingExit_confirmed_settlesAgainstLinkedEntry() {
        LiveOrder exit = pending("COVR", 1);
        exit.setEntryOrderId(7L);
        LiveOrder entry = new LiveOrder();
        entry.setId(7L);
        entry.setFilledQuantity(new BigDecimal("0.0029"));
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(exit));
        when(repo.findById(7L)).thenReturn(Optional.of(entry));
        KrakenFuturesOrderClient.Fill fill = new KrakenFuturesOrderClient.Fill(new BigDecimal("0.0029"), new BigDecimal("85000"));
        when(client.findFill("ord-1")).thenReturn(fill);

        reconciler.reconcile();

        verify(executor).settleExit(entry, exit, fill.size(), fill.averagePrice());
    }

    @Test
    void noFill_withinWindow_staysPending() {
        LiveOrder entry = pending("SHRT", 5);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(entry));
        when(client.findFill("ord-1")).thenReturn(null);

        reconciler.reconcile();

        assertEquals("PENDING", entry.getStatus());
        verify(repo, never()).save(any());
    }

    @Test
    void noFill_afterWindow_becomesUnresolved() {
        LiveOrder entry = pending("SHRT", 45);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(entry));
        when(client.findFill("ord-1")).thenReturn(null);

        reconciler.reconcile();

        assertEquals("UNRESOLVED", entry.getStatus());
        verify(repo).save(entry);
    }
}
