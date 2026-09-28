package nu.itark.frosk.crypto.livetrading;

import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.model.LiveOrder;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Order bookkeeping around the broker: unconfirmed fills, the opposite-direction
 * (netting) guard, partial closes — and that Coinbase's PENDING semantics are
 * unchanged.
 */
public class TestJLiveOrderExecutor {

    private static final String TICKER = "PF_XBTUSD";
    private static final String STRATEGY = "CryptoShortIntradayStrategy";

    private LiveTradingGate gate;
    private BrokerOrderClient broker;
    private LiveOrderRepository repo;
    private LiveOrderExecutor executor;

    @BeforeEach
    void setUp() {
        gate = mock(LiveTradingGate.class);
        broker = mock(BrokerOrderClient.class);
        repo = mock(LiveOrderRepository.class);
        executor = new LiveOrderExecutor(gate, broker, repo, mock(IntradaySignalRepository.class));
        when(broker.supportsShort()).thenReturn(true);
        when(gate.computePositionSizeEur(any())).thenReturn(new BigDecimal("250"));
        when(gate.canTrade(anyString(), any())).thenReturn(true);
        when(repo.findByTickerAndSideInAndStatusIn(anyString(), anyCollection(), anyCollection())).thenReturn(List.of());
    }

    private static OrderResponse resp(String status, String size, String price) {
        OrderResponse r = new OrderResponse();
        r.setStatus(status);
        r.setOrderId("ord-x");
        r.setFilledSize(size == null ? null : new BigDecimal(size));
        r.setAverageFilledPrice(price == null ? null : new BigDecimal(price));
        return r;
    }

    private static LiveOrder filledShortEntry(String qty) {
        LiveOrder e = new LiveOrder();
        e.setId(7L);
        e.setTicker(TICKER);
        e.setSide("SHRT");
        e.setStrategyName(STRATEGY);
        e.setStatus("FILLED");
        e.setFilledQuantity(new BigDecimal(qty));
        e.setFilledPrice(new BigDecimal("86000"));
        e.setExecutionMode("LIVE");
        return e;
    }

    /** Every LiveOrder passed to save(), in order (rows are mutable, so this is their final state). */
    private List<LiveOrder> saved() {
        ArgumentCaptor<LiveOrder> captor = ArgumentCaptor.forClass(LiveOrder.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    @Test
    void unconfirmedEntry_isSavedPending_withoutQuantity() {
        when(broker.placeShortEntry(TICKER, new BigDecimal("250"))).thenReturn(resp("UNCONFIRMED", null, null));

        executor.dispatch("SHRT", STRATEGY, TICKER, new BigDecimal("85000"), null, null, "LIVE");

        LiveOrder row = saved().get(0);
        assertEquals("PENDING", row.getStatus());
        assertNull(row.getFilledQuantity());
        assertNull(row.getFilledPrice(), "must not fall back to the bar close as a fill price");
    }

    @Test
    void entryOppositeToAnotherStrategysOpenPosition_isRefused() {
        LiveOrder longHeld = new LiveOrder();
        longHeld.setStrategyName("CryptoVWAPReversionIntradayStrategy");
        when(repo.findByTickerAndSideInAndStatusIn(eq(TICKER), eq(List.of("BUY")), anyCollection()))
                .thenReturn(List.of(longHeld));

        executor.dispatch("SHRT", STRATEGY, TICKER, new BigDecimal("85000"), null, null, "LIVE");

        verify(broker, never()).placeShortEntry(anyString(), any());
        verify(repo, never()).save(any());
    }

    @Test
    void exit_closesFilledEntry_andSettlesPnl() {
        LiveOrder entry = filledShortEntry("0.0029");
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(TICKER, STRATEGY, "SHRT", "FILLED"))
                .thenReturn(Optional.of(entry));
        when(broker.placeShortExit(TICKER, new BigDecimal("0.0029"))).thenReturn(resp("FILLED", "0.0029", "85000"));

        executor.dispatch("COVR", STRATEGY, TICKER, new BigDecimal("85000"), null, null, "PAPER");

        assertEquals("CLOSED", entry.getStatus());
        LiveOrder exit = saved().stream().filter(o -> "COVR".equals(o.getSide())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("2.9").compareTo(exit.getRealizedPnlEur())); // (86000−85000)×0.0029
        assertEquals(7L, exit.getEntryOrderId());
        assertEquals("LIVE", exit.getExecutionMode(), "exit carries its entry's mode after a rollback");
    }

    @Test
    void partialExit_leavesRemainderOpen() {
        LiveOrder entry = filledShortEntry("0.0029");
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(TICKER, STRATEGY, "SHRT", "FILLED"))
                .thenReturn(Optional.of(entry));
        when(broker.placeShortExit(any(), any())).thenReturn(resp("FILLED", "0.0020", "85000"));

        executor.dispatch("COVR", STRATEGY, TICKER, new BigDecimal("85000"), null, null, "LIVE");

        assertEquals("FILLED", entry.getStatus());
        assertEquals(0, new BigDecimal("0.0009").compareTo(entry.getFilledQuantity()));
    }

    @Test
    void unconfirmedExit_marksEntryClosing_soItIsNotClosedTwice() {
        LiveOrder entry = filledShortEntry("0.0029");
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(TICKER, STRATEGY, "SHRT", "FILLED"))
                .thenReturn(Optional.of(entry));
        when(broker.placeShortExit(any(), any())).thenReturn(resp("UNCONFIRMED", null, null));

        executor.dispatch("COVR", STRATEGY, TICKER, new BigDecimal("85000"), null, null, "LIVE");

        assertEquals("CLOSING", entry.getStatus());
        LiveOrder exit = saved().stream().filter(o -> "COVR".equals(o.getSide())).findFirst().orElseThrow();
        assertEquals("PENDING", exit.getStatus());
        assertEquals(7L, exit.getEntryOrderId());
    }

    @Test
    void coinbasePendingExit_stillClosesTheWholeQuantity() {
        // Regression guard: Coinbase PENDING with filledSize 0 has always meant "whole quantity".
        LiveOrder entry = filledShortEntry("1.5");
        entry.setSide("BUY");
        entry.setFilledPrice(new BigDecimal("100"));
        when(repo.findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc("BTC-EUR", STRATEGY, "BUY", "FILLED"))
                .thenReturn(Optional.of(entry));
        entry.setTicker("BTC-EUR");
        when(broker.placeLongExit(any(), any())).thenReturn(resp("PENDING", "0", "110"));

        executor.dispatch("SELL", STRATEGY, "BTC-EUR", new BigDecimal("110"), null, null, null);

        assertEquals("CLOSED", entry.getStatus());
        LiveOrder exit = saved().stream().filter(o -> "SELL".equals(o.getSide())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("15").compareTo(exit.getRealizedPnlEur())); // (110−100)×1.5
    }
}
