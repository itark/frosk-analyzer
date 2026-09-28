package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Entry point from {@code CryptoIntradayStrategyRunner} on the kraken-futures
 * profile: picks the {@link KrakenOrderDispatcher} for a signal from its
 * strategy's {@link StrategyMode}.
 *
 * <p><b>Entries</b> go where the mode says: PAPER → paper, SHADOW → both,
 * LIVE → Kraken, DISABLED → nowhere.
 *
 * <p><b>Exits</b> go to both venues regardless of mode. Each venue closes the
 * matching open position if it has one and does nothing otherwise, so routing
 * exits everywhere is safe — and it is what stops mode changes from stranding
 * positions: a real position opened while LIVE is still closed by its exit
 * signal after a rollback to PAPER, and a paper position opened while PAPER is
 * still closed after a promotion to LIVE.
 */
@Component
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenOrderRouter {

    private final KrakenStrategyModeService modeService;
    private final PaperKrakenOrderDispatcher paperDispatcher;
    private final LiveKrakenOrderDispatcher liveDispatcher;
    private final ShadowKrakenOrderDispatcher shadowDispatcher;

    public void route(OrderRequest order) {
        StrategyMode mode = modeService.getMode(order.strategyName());

        if (!order.isEntry()) {
            // Tag argument only matters when the live entry predates execution_mode;
            // LiveOrderExecutor otherwise reuses the entry's own tag.
            paperDispatcher.dispatch(order, StrategyMode.PAPER);
            liveDispatcher.dispatch(order, mode.opensLive() ? mode : StrategyMode.LIVE);
            return;
        }

        KrakenOrderDispatcher dispatcher = dispatcherFor(mode);
        if (dispatcher == null) {
            log.info("KrakenOrderRouter: {} is DISABLED — dropping {} {}",
                    order.strategyName(), order.signalType(), order.ticker());
            return;
        }
        dispatcher.dispatch(order);
    }

    /** The entry dispatcher for {@code mode}; null for DISABLED. */
    KrakenOrderDispatcher dispatcherFor(StrategyMode mode) {
        return switch (mode) {
            case PAPER    -> paperDispatcher;
            case SHADOW   -> shadowDispatcher;
            case LIVE     -> liveDispatcher;
            case DISABLED -> null;
        };
    }
}
