package nu.itark.frosk.crypto.coinbase.orderbook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pure unit test (no Spring context, no network) for the Cont-Kukanov-Stoikov
 * OFI formula locked in PREREG_ofi_1m.md §4. Each test isolates one term of
 * the formula so a future change to the arithmetic fails at the specific
 * case it breaks, not just in aggregate.
 */
public class TestJOrderBookState {

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private OrderBookState seeded() {
        OrderBookState state = new OrderBookState();
        state.applySnapshot(List.of(
                new OrderBookState.Level2Update("bid", d("100.00"), d("1.0")),
                new OrderBookState.Level2Update("bid", d("99.00"), d("5.0")),
                new OrderBookState.Level2Update("offer", d("101.00"), d("2.0")),
                new OrderBookState.Level2Update("offer", d("102.00"), d("5.0"))
        ));
        return state;
    }

    @Test
    void snapshotContributesNoOfi() {
        OrderBookState state = seeded();
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, BigDecimal.ZERO.compareTo(flush.ofiSum()));
        assertEquals(0, flush.eventCount());
        assertEquals(0, d("100.00").compareTo(flush.bidPrice()));
        assertEquals(0, d("101.00").compareTo(flush.askPrice()));
    }

    @Test
    void bidPriceImprovementAddsNewBidSize() {
        // A new, higher best bid appears (100.00 -> 100.50): OFI = +newBidSize.
        OrderBookState state = seeded();
        state.applyUpdate("bid", d("100.50"), d("3.0"));
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, d("3.0").compareTo(flush.ofiSum()));
        assertEquals(1, flush.eventCount());
    }

    @Test
    void bidSizeIncreaseAtSamePriceAddsTheIncrease() {
        // Best bid price unchanged, size grows 1.0 -> 4.0: OFI = +3.0.
        OrderBookState state = seeded();
        state.applyUpdate("bid", d("100.00"), d("4.0"));
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, d("3.0").compareTo(flush.ofiSum()));
    }

    @Test
    void bidDisappearingSubtractsTheLostSize() {
        // Best bid (100.00, size 1.0) is fully cancelled; next best is 99.00.
        // Price drops (worse), so OFI = -prevBidSize = -1.0.
        OrderBookState state = seeded();
        state.applyUpdate("bid", d("100.00"), BigDecimal.ZERO);
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, d("-1.0").compareTo(flush.ofiSum()));
        assertEquals(0, d("99.00").compareTo(flush.bidPrice()));
    }

    @Test
    void askPriceImprovementAddsNewAskSize() {
        // A new, lower best ask appears (101.00 -> 100.75): more sell pressure
        // at a better price is bearish, so OFI = -newAskSize.
        OrderBookState state = seeded();
        state.applyUpdate("offer", d("100.75"), d("2.5"));
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, d("-2.5").compareTo(flush.ofiSum()));
    }

    @Test
    void askDisappearingAddsTheLostSize() {
        // Best ask (101.00, size 2.0) is fully cancelled; next best is 102.00.
        // Losing sell-side resistance is bullish, so OFI = +prevAskSize = +2.0.
        OrderBookState state = seeded();
        state.applyUpdate("offer", d("101.00"), BigDecimal.ZERO);
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, d("2.0").compareTo(flush.ofiSum()));
        assertEquals(0, d("102.00").compareTo(flush.askPrice()));
    }

    @Test
    void updateToANonTopLevelContributesZero() {
        // A deeper bid level changes; best bid/ask are untouched, so P_n=P_n-1
        // and q_n=q_n-1 at the top and the formula degenerates to zero.
        OrderBookState state = seeded();
        state.applyUpdate("bid", d("99.00"), d("7.0")); // not the best bid
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, BigDecimal.ZERO.compareTo(flush.ofiSum()));
        assertEquals(1, flush.eventCount(), "still counted as an observed event, just a zero-contribution one");
    }

    @Test
    void flushResetsAccumulatorButKeepsBookState() {
        OrderBookState state = seeded();
        state.applyUpdate("bid", d("100.50"), d("3.0"));
        state.flushMinute();

        // Next minute starts from an empty accumulator...
        state.applyUpdate("bid", d("100.50"), d("6.0")); // size increase 3.0 -> 6.0 at the now-best bid
        OrderBookState.MinuteFlush secondFlush = state.flushMinute();
        assertEquals(0, d("3.0").compareTo(secondFlush.ofiSum()),
                "must reflect only this minute's change, not carry over the prior minute's OFI");
        // ...but book depth carried over: best bid is still 100.50, not reset to 100.00.
        assertEquals(0, d("100.50").compareTo(secondFlush.bidPrice()));
    }

    @Test
    void emptyBookBeforeSnapshotSkipsSafely() {
        OrderBookState state = new OrderBookState();
        // No snapshot applied yet — book is one-sided or empty; must not throw.
        state.applyUpdate("bid", d("100.00"), d("1.0"));
        OrderBookState.MinuteFlush flush = state.flushMinute();
        assertEquals(0, BigDecimal.ZERO.compareTo(flush.ofiSum()));
        assertNull(flush.askPrice());
    }
}
