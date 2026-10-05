package nu.itark.frosk.crypto.coinbase.lifecycle;

import nu.itark.frosk.crypto.kraken.lifecycle.OrderRequest;

/**
 * Sends a signal's order to one or more venues on the crypto (Coinbase)
 * process. {@link CoinbaseOrderRouter} picks the implementation per strategy
 * from its {@link nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode}.
 *
 * <p>Mirrors {@code KrakenOrderDispatcher}. Reuses {@link OrderRequest} as-is
 * — despite living in the {@code kraken.lifecycle} package it is a plain
 * (signalType, strategyName, ticker, closePrice, signal, strength) tuple with
 * nothing Kraken-specific in it.
 */
public interface CoinbaseOrderDispatcher {

    void dispatch(OrderRequest order);
}
