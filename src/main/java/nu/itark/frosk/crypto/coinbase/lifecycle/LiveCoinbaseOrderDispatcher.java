package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.crypto.kraken.lifecycle.OrderRequest;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import nu.itark.frosk.crypto.livetrading.LiveOrderExecutor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Real order on Coinbase via {@link LiveOrderExecutor} (the pre-existing live
 * logic, shared with the kraken-futures process). Entries still pass
 * {@code LiveTradingGate}, so a LIVE strategy places nothing while
 * {@code crypto.live.trading.enabled=false}. Mirrors {@code LiveKrakenOrderDispatcher}.
 */
@Component
@Profile("crypto")
@RequiredArgsConstructor
public class LiveCoinbaseOrderDispatcher implements CoinbaseOrderDispatcher {

    private final LiveOrderExecutor liveOrderExecutor;

    @Override
    public void dispatch(OrderRequest order) {
        dispatch(order, StrategyMode.LIVE);
    }

    /** As {@link #dispatch(OrderRequest)}, tagging the {@code live_order} row with {@code mode} (LIVE or SHADOW). */
    void dispatch(OrderRequest order, StrategyMode mode) {
        liveOrderExecutor.dispatch(order.signalType(), order.strategyName(), order.ticker(),
                order.closePrice(), order.signal(), order.strength(), mode.name());
    }
}
