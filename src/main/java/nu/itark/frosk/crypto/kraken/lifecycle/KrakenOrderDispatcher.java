package nu.itark.frosk.crypto.kraken.lifecycle;

/**
 * Sends a signal's order to one or more venues. {@link KrakenOrderRouter} picks
 * the implementation per strategy from its {@link StrategyMode}.
 */
public interface KrakenOrderDispatcher {

    void dispatch(OrderRequest order);
}
