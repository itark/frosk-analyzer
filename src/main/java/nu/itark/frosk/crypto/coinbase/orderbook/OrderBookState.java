package nu.itark.frosk.crypto.coinbase.orderbook;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Maintains one product's live order-book depth from a stream of Coinbase
 * level2 update events, and accumulates order flow imbalance (OFI) at the
 * top of book using the discrete formulation from Cont, Kukanov &amp; Stoikov
 * (2014), <i>"The Price Impact of Order Book Events"</i> — locked as §4 of
 * {@code ~/itark/PREREG_ofi_1m.md}:
 *
 * <pre>
 * ΔW_bid = q_bid,n · 1{P_bid,n ≥ P_bid,n-1} − q_bid,n-1 · 1{P_bid,n ≤ P_bid,n-1}
 * ΔW_ask = q_ask,n · 1{P_ask,n ≤ P_ask,n-1} − q_ask,n-1 · 1{P_ask,n ≥ P_ask,n-1}
 * OFI_n  = ΔW_bid − ΔW_ask
 * </pre>
 *
 * <p>Applied after <b>every</b> update event, not only events that touch the
 * top level — an update to a non-top level leaves best price and size
 * unchanged, so {@code P_n = P_n-1} and {@code q_n = q_n-1} and the formula
 * contributes exactly zero on its own. This avoids needing a separate
 * "did this event change the top of book" branch; the formula already
 * degenerates to a no-op when nothing relevant changed.
 *
 * <p>One instance per product. Mutating methods are called from the
 * WebSocket reader thread; {@link #flushMinute()} is called once a minute
 * from a scheduler thread — both cross into the same mutable state, so the
 * public methods are synchronized on {@code this} rather than assuming a
 * single-threaded caller.
 */
public class OrderBookState {

    private final NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();

    private BigDecimal prevBidPrice;
    private BigDecimal prevBidSize;
    private BigDecimal prevAskPrice;
    private BigDecimal prevAskSize;

    private BigDecimal minuteOfiSum = BigDecimal.ZERO;
    private int minuteEventCount = 0;

    /**
     * Applies a full snapshot (the level2 channel's initial "snapshot" event):
     * replaces the book outright and anchors {@code prev*} to it, without
     * contributing to OFI — there is no meaningful "previous" state to compare
     * a snapshot against, and doing so would manufacture a spurious flow event
     * out of the initial book population.
     */
    public synchronized void applySnapshot(java.util.List<Level2Update> updates) {
        bids.clear();
        asks.clear();
        for (Level2Update u : updates) {
            book(u.side()).put(u.priceLevel(), u.newQuantity());
        }
        Map.Entry<BigDecimal, BigDecimal> bestBid = bids.firstEntry();
        Map.Entry<BigDecimal, BigDecimal> bestAsk = asks.firstEntry();
        prevBidPrice = bestBid != null ? bestBid.getKey() : null;
        prevBidSize = bestBid != null ? bestBid.getValue() : null;
        prevAskPrice = bestAsk != null ? bestAsk.getKey() : null;
        prevAskSize = bestAsk != null ? bestAsk.getValue() : null;
    }

    /**
     * Applies one incremental level2 update event and accumulates its OFI
     * contribution into the current minute's running sum.
     *
     * @param side        "bid" or "offer"/"ask" (Coinbase uses "bid"/"offer")
     * @param priceLevel  the price level this update applies to
     * @param newQuantity the new resting size at that level — zero removes it
     */
    public synchronized void applyUpdate(String side, BigDecimal priceLevel, BigDecimal newQuantity) {
        NavigableMap<BigDecimal, BigDecimal> book = book(side);
        if (newQuantity.signum() == 0) {
            book.remove(priceLevel);
        } else {
            book.put(priceLevel, newQuantity);
        }

        Map.Entry<BigDecimal, BigDecimal> bestBid = bids.firstEntry();
        Map.Entry<BigDecimal, BigDecimal> bestAsk = asks.firstEntry();
        if (bestBid == null || bestAsk == null) {
            // Book not populated on both sides yet (e.g. right after connecting,
            // before a snapshot has fully landed) — nothing to compare against.
            return;
        }
        BigDecimal bidPrice = bestBid.getKey();
        BigDecimal bidSize = bestBid.getValue();
        BigDecimal askPrice = bestAsk.getKey();
        BigDecimal askSize = bestAsk.getValue();

        if (prevBidPrice != null && prevAskPrice != null) {
            BigDecimal deltaWBid = bidPrice.compareTo(prevBidPrice) >= 0
                    ? bidSize : BigDecimal.ZERO;
            if (bidPrice.compareTo(prevBidPrice) <= 0) {
                deltaWBid = deltaWBid.subtract(prevBidSize);
            }
            BigDecimal deltaWAsk = askPrice.compareTo(prevAskPrice) <= 0
                    ? askSize : BigDecimal.ZERO;
            if (askPrice.compareTo(prevAskPrice) >= 0) {
                deltaWAsk = deltaWAsk.subtract(prevAskSize);
            }
            minuteOfiSum = minuteOfiSum.add(deltaWBid).subtract(deltaWAsk);
            minuteEventCount++;
        }

        prevBidPrice = bidPrice;
        prevBidSize = bidSize;
        prevAskPrice = askPrice;
        prevAskSize = askSize;
    }

    /**
     * Returns the current minute's accumulated state and resets the
     * accumulator — book depth and {@code prev*} carry over unchanged, only
     * {@code ofiSum}/{@code eventCount} reset, matching §4's definition of the
     * per-minute value as a sum over that minute, not a running total.
     */
    public synchronized MinuteFlush flushMinute() {
        Map.Entry<BigDecimal, BigDecimal> bestBid = bids.firstEntry();
        Map.Entry<BigDecimal, BigDecimal> bestAsk = asks.firstEntry();
        MinuteFlush flush = new MinuteFlush(
                bestBid != null ? bestBid.getKey() : null,
                bestBid != null ? bestBid.getValue() : null,
                bestAsk != null ? bestAsk.getKey() : null,
                bestAsk != null ? bestAsk.getValue() : null,
                minuteOfiSum,
                minuteEventCount
        );
        minuteOfiSum = BigDecimal.ZERO;
        minuteEventCount = 0;
        return flush;
    }

    private NavigableMap<BigDecimal, BigDecimal> book(String side) {
        return "bid".equalsIgnoreCase(side) ? bids : asks;
    }

    public record Level2Update(String side, BigDecimal priceLevel, BigDecimal newQuantity) {
    }

    public record MinuteFlush(BigDecimal bidPrice, BigDecimal bidSize,
                              BigDecimal askPrice, BigDecimal askSize,
                              BigDecimal ofiSum, int eventCount) {
    }
}
