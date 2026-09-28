package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.service.KrakenFuturesPaperTradingService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Simulated fill against the Kraken Futures paper account — the pre-existing
 * paper behaviour, unchanged apart from the execution-mode tag on new positions.
 */
@Component
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class PaperKrakenOrderDispatcher implements KrakenOrderDispatcher {

    private final KrakenFuturesPaperTradingService paperTradingService;

    @Override
    public void dispatch(OrderRequest order) {
        dispatch(order, StrategyMode.PAPER);
    }

    /** As {@link #dispatch(OrderRequest)}, tagging new positions with {@code mode} (PAPER or SHADOW). */
    void dispatch(OrderRequest order, StrategyMode mode) {
        String tag = mode.name();
        switch (order.signalType()) {
            case "BUY"  -> paperTradingService.dispatchLong(order.strategyName(), order.ticker(),
                    order.closePrice(), order.strength(), tag);
            case "SELL" -> paperTradingService.dispatchCloseLong(order.strategyName(), order.ticker(), order.closePrice());
            case "SHRT" -> paperTradingService.dispatchShort(order.strategyName(), order.ticker(),
                    order.closePrice(), order.strength(), tag);
            case "COVR" -> paperTradingService.dispatchCloseShort(order.strategyName(), order.ticker(), order.closePrice());
            default     -> log.warn("PaperKrakenOrderDispatcher: unknown signal type '{}'", order.signalType());
        }
    }
}
