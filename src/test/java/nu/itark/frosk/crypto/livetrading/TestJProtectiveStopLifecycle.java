package nu.itark.frosk.crypto.livetrading;

import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.broker.ProtectiveStopClient;
import nu.itark.frosk.crypto.kraken.KrakenFuturesLiveOrderReconciler;
import nu.itark.frosk.crypto.kraken.KrakenFuturesOrderClient;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lifecycle of the exchange-side stop: attached when an entry fills, cancelled
 * when the strategy closes normally, and — when it fires instead — detected and
 * settled so the app does not keep treating the position as open.
 */
public class TestJProtectiveStopLifecycle {

    private static final String TICKER = "PF_XBTUSD";
    private static final String STRATEGY = "CryptoVWAPReversionIntradayStrategy";

    /** Mockito needs one type implementing both broker roles, as Kraken's client does. */
    private interface StopCapableBroker extends BrokerOrderClient, ProtectiveStopClient {}

    private StopCapableBroker broker;
    private LiveOrderRepository repo;
    private LiveOrderExecutor executor;

    @BeforeEach
    void setUp() {
        broker = mock(StopCapableBroker.class);
        repo = mock(LiveOrderRepository.class);
        LiveTradingGate gate = mock(LiveTradingGate.class);
        when(gate.computePositionSizeEur(any())).thenReturn(new BigDecimal("250"));
        when(gate.canTrade(anyString(), any())).thenReturn(true);
        when(broker.supportsShort()).thenReturn(true);
        when(repo.findByTickerAndSideInAndStatusIn(anyString(), anyCollection(), anyCollection())).thenReturn(List.of());

        executor = new LiveOrderExecutor(gate, broker, repo, mock(IntradaySignalRepository.class));
        ReflectionTestUtils.setField(executor, "protectiveStopPct", new BigDecimal("2.7"));
    }

    private static OrderResponse filled(String size, String price) {
        OrderResponse r = new OrderResponse();
        r.setStatus("FILLED");
        r.setOrderId("entry-1");
        r.setFilledSize(new BigDecimal(size));
        r.setAverageFilledPrice(new BigDecimal(price));
        return r;
    }

    private static OrderResponse stopPlaced(String id) {
        OrderResponse r = new OrderResponse();
        r.setStatus("PLACED");
        r.setOrderId(id);
        return r;
    }

    private static LiveOrder openEntryWithStop() {
        LiveOrder e = new LiveOrder();
        e.setId(7L);
        e.setTicker(TICKER);
        e.setSide("BUY");
        e.setStrategyName(STRATEGY);
        e.setStatus("FILLED");
        e.setFilledQuantity(new BigDecimal("0.0029"));
        e.setFilledPrice(new BigDecimal("85693.71"));
        e.setExecutionMode("LIVE");
        e.setProtectiveStopOrderId("stop-1");
        return e;
    }

    private List<LiveOrder> saved() {
        ArgumentCaptor<LiveOrder> captor = ArgumentCaptor.forClass(LiveOrder.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    @Test
    void filledEntry_getsARestingStopAtTheFillPrice() {
        when(broker.placeLongEntry(eq(TICKER), any())).thenReturn(filled("0.0029", "85693.71"));
        when(broker.placeProtectiveStop(eq(TICKER), eq(true), any(), any(), any())).thenReturn(stopPlaced("stop-9"));

        executor.dispatch("BUY", STRATEGY, TICKER, new BigDecimal("85700"), null, null, "LIVE");

        // Stop is measured from the actual fill price, not the bar close.
        verify(broker).placeProtectiveStop(TICKER, true, new BigDecimal("0.0029"),
                new BigDecimal("85693.71"), new BigDecimal("2.7"));
        assertEquals("stop-9", saved().get(saved().size() - 1).getProtectiveStopOrderId());
    }

    @Test
    void entryWithoutAConfirmedFill_getsNoStopYet() {
        OrderResponse unconfirmed = new OrderResponse();
        unconfirmed.setStatus("UNCONFIRMED");
        unconfirmed.setOrderId("entry-2");
        when(broker.placeLongEntry(eq(TICKER), any())).thenReturn(unconfirmed);

        executor.dispatch("BUY", STRATEGY, TICKER, new BigDecimal("85700"), null, null, "LIVE");

        verify(broker, never()).placeProtectiveStop(anyString(), anyBoolean(), any(), any(), any());
    }

    @Test
    void stopIsDisabledByZeroPct() {
        ReflectionTestUtils.setField(executor, "protectiveStopPct", BigDecimal.ZERO);
        when(broker.placeLongEntry(eq(TICKER), any())).thenReturn(filled("0.0029", "85693.71"));

        executor.dispatch("BUY", STRATEGY, TICKER, new BigDecimal("85700"), null, null, "LIVE");

        verify(broker, never()).placeProtectiveStop(anyString(), anyBoolean(), any(), any(), any());
    }

    @Test
    void normalExit_cancelsTheRestingStopBeforeClosing() {
        LiveOrder entry = openEntryWithStop();
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(TICKER, STRATEGY, "BUY", "FILLED"))
                .thenReturn(Optional.of(entry));
        when(broker.cancelOrder("stop-1")).thenReturn(true);
        when(broker.placeLongExit(any(), any())).thenReturn(filled("0.0029", "86000"));

        executor.dispatch("SELL", STRATEGY, TICKER, new BigDecimal("86000"), null, null, "LIVE");

        verify(broker).cancelOrder("stop-1");
        assertNull(entry.getProtectiveStopOrderId(), "a cancelled stop must not stay on the row");
        assertEquals("CLOSED", entry.getStatus());
    }

    @Test
    void failedCancel_keepsTheStopIdSoTheReconcilerStillWatchesIt() {
        LiveOrder entry = openEntryWithStop();
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(TICKER, STRATEGY, "BUY", "FILLED"))
                .thenReturn(Optional.of(entry));
        when(broker.cancelOrder("stop-1")).thenReturn(false);
        when(broker.placeLongExit(any(), any())).thenReturn(filled("0.0029", "86000"));

        executor.dispatch("SELL", STRATEGY, TICKER, new BigDecimal("86000"), null, null, "LIVE");

        assertEquals("stop-1", entry.getProtectiveStopOrderId());
    }

    @Test
    void firedStop_isDetectedAndSettledAgainstTheEntry() {
        LiveOrder entry = openEntryWithStop();
        KrakenFuturesOrderClient krakenClient = mock(KrakenFuturesOrderClient.class);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of());
        when(repo.findByStatusAndProtectiveStopOrderIdIsNotNull("FILLED")).thenReturn(List.of(entry));
        when(krakenClient.findFill("stop-1")).thenReturn(
                new KrakenFuturesOrderClient.Fill(new BigDecimal("0.0029"), new BigDecimal("83379")));

        LiveOrderExecutor realExecutor = new LiveOrderExecutor(
                mock(LiveTradingGate.class), broker, repo, mock(IntradaySignalRepository.class));
        new KrakenFuturesLiveOrderReconciler(repo, krakenClient, realExecutor, mock(IntradaySignalRepository.class))
                .reconcile();

        assertEquals("CLOSED", entry.getStatus());
        assertNull(entry.getProtectiveStopOrderId());
        LiveOrder exit = saved().stream().filter(o -> "SELL".equals(o.getSide())).findFirst().orElseThrow();
        assertEquals(7L, exit.getEntryOrderId());
        assertEquals("stop-1", exit.getCoinbaseOrderId());
        // (83379 − 85693.71) × 0.0029 ≈ −6.71, i.e. the 2.7 % stop, not a 15m bar-close loss
        assertEquals(0, new BigDecimal("-6.7126590").compareTo(exit.getRealizedPnlEur()));
    }

    @Test
    void openEntryWithNoFillOnItsStop_isLeftAlone() {
        LiveOrder entry = openEntryWithStop();
        KrakenFuturesOrderClient krakenClient = mock(KrakenFuturesOrderClient.class);
        when(repo.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of());
        when(repo.findByStatusAndProtectiveStopOrderIdIsNotNull("FILLED")).thenReturn(List.of(entry));
        when(krakenClient.findFill("stop-1")).thenReturn(null);

        new KrakenFuturesLiveOrderReconciler(repo, krakenClient, executor, mock(IntradaySignalRepository.class))
                .reconcile();

        assertEquals("FILLED", entry.getStatus());
        assertEquals("stop-1", entry.getProtectiveStopOrderId());
        verify(repo, never()).save(any());
    }
}
