package nu.itark.frosk.broker;

import nu.itark.frosk.crypto.livetrading.OrderResponse;

import java.math.BigDecimal;

/**
 * A broker that can rest a protective stop order on the exchange alongside an
 * open position. Implemented by Kraken Futures; Coinbase spot does not, so
 * {@code LiveOrderExecutor} only places stops when its broker implements this.
 *
 * <p>The point is intrabar protection: the strategies' own stops are evaluated
 * on 15-minute bar closes, so a fast move is only exited at that bar's close.
 */
public interface ProtectiveStopClient {

    /**
     * Rests a reduce-only stop that closes {@code size} of an open position if
     * price moves {@code stopPct} percent against {@code entryPrice}.
     *
     * @param isLong direction of the position being protected
     * @return status {@code PLACED} with the order id, or {@code FAILED}
     */
    OrderResponse placeProtectiveStop(String symbol, boolean isLong, BigDecimal size,
                                      BigDecimal entryPrice, BigDecimal stopPct);

    /** Cancels a resting stop; true when no resting stop remains (cancelled, filled or unknown to the exchange). */
    boolean cancelOrder(String orderId);
}
