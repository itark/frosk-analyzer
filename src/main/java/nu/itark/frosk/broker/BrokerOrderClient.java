package nu.itark.frosk.broker;

import nu.itark.frosk.crypto.livetrading.OrderResponse;

import java.math.BigDecimal;

/**
 * Broker-agnostic interface for live order placement and account queries.
 *
 * <p>Coinbase Advanced Trade (spot, long-only) and Kraken Futures (margin,
 * long + short) are the two concrete implementations. The active bean is
 * determined by the Spring profile: {@code crypto} → CoinbaseOrderClient,
 * {@code kraken-futures} → KrakenFuturesOrderClient.
 *
 * <p>Entry methods receive the EUR-equivalent notional to invest; each
 * implementation converts internally to the exchange's native unit (coins for
 * Coinbase, contracts for Kraken Futures). Exit methods receive the quantity
 * in those same native units so the full open position can be closed exactly.
 */
public interface BrokerOrderClient {

    /**
     * Open a long position — spend {@code eurAmount} EUR (or equivalent margin).
     *
     * @param symbol    exchange product identifier (e.g. "BTC-EUR" or "PF_XBTUSD")
     * @param eurAmount EUR-equivalent notional to spend on entry
     */
    OrderResponse placeLongEntry(String symbol, BigDecimal eurAmount);

    /**
     * Close a long position — sell {@code quantity} units (coins or contracts).
     *
     * @param symbol   exchange product identifier
     * @param quantity native units to sell (coins for Coinbase, contracts for Kraken)
     */
    OrderResponse placeLongExit(String symbol, BigDecimal quantity);

    /**
     * Open a short position — sell short {@code eurAmount} EUR notional.
     *
     * <p>Not supported on Coinbase; supported natively on Kraken Futures.
     *
     * @param symbol    exchange product identifier
     * @param eurAmount EUR-equivalent notional for the short position
     */
    OrderResponse placeShortEntry(String symbol, BigDecimal eurAmount);

    /**
     * Close a short position (cover) — buy back {@code quantity} units.
     *
     * @param symbol   exchange product identifier
     * @param quantity native units to buy back (contracts for Kraken Futures)
     */
    OrderResponse placeShortExit(String symbol, BigDecimal quantity);

    /**
     * Available liquid balance to place new orders.
     *
     * <p>For Coinbase: available EUR cash. For Kraken Futures: available margin
     * (flexible collateral available for new positions).
     */
    BigDecimal getAvailableBalance();

    /**
     * Whether this broker supports short selling.
     * Defaults to {@code false}; Kraken Futures overrides to {@code true}.
     */
    default boolean supportsShort() {
        return false;
    }
}
