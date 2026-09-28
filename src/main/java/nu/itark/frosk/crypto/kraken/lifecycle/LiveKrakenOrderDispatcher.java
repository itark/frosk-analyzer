package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.crypto.livetrading.LiveOrderExecutor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Real order on Kraken Futures via {@link LiveOrderExecutor} (the pre-existing
 * live logic). Entries still pass {@code LiveTradingGate}, so a LIVE strategy
 * places nothing while {@code crypto.live.trading.enabled=false}.
 */
@Component
@Profile("kraken-futures")
@RequiredArgsConstructor
public class LiveKrakenOrderDispatcher implements KrakenOrderDispatcher {

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
