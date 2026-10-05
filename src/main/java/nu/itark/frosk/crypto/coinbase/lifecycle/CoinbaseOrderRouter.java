package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.kraken.lifecycle.OrderRequest;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Entry point from {@code CryptoIntradayStrategyRunner} on the crypto
 * (Coinbase) profile: picks the {@link CoinbaseOrderDispatcher} for a signal
 * from its strategy's {@link StrategyMode}. Mirrors {@code KrakenOrderRouter}.
 *
 * <p><b>Entries</b> go where the mode says: PAPER → paper, SHADOW → both,
 * LIVE → Coinbase, DISABLED → nowhere.
 *
 * <p><b>Exits</b> go to both venues regardless of mode. Each venue closes the
 * matching open position if it has one and does nothing otherwise, so routing
 * exits everywhere is safe — and it is what stops mode changes from stranding
 * positions: a real position opened while LIVE is still closed by its exit
 * signal after a rollback to PAPER, and a paper position opened while PAPER is
 * still closed after a promotion to LIVE.
 */
@Component
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class CoinbaseOrderRouter {

    private final CoinbaseStrategyModeService modeService;
    private final PaperCoinbaseOrderDispatcher paperDispatcher;
    private final LiveCoinbaseOrderDispatcher liveDispatcher;
    private final ShadowCoinbaseOrderDispatcher shadowDispatcher;

    public void route(OrderRequest order) {
        StrategyMode mode = modeService.getMode(order.strategyName());

        if (!order.isEntry()) {
            paperDispatcher.dispatch(order, StrategyMode.PAPER);
            liveDispatcher.dispatch(order, mode.opensLive() ? mode : StrategyMode.LIVE);
            return;
        }

        CoinbaseOrderDispatcher dispatcher = dispatcherFor(mode);
        if (dispatcher == null) {
            log.info("CoinbaseOrderRouter: {} is DISABLED — dropping {} {}",
                    order.strategyName(), order.signalType(), order.ticker());
            return;
        }
        dispatcher.dispatch(order);
    }

    /** The entry dispatcher for {@code mode}; null for DISABLED. */
    CoinbaseOrderDispatcher dispatcherFor(StrategyMode mode) {
        return switch (mode) {
            case PAPER    -> paperDispatcher;
            case SHADOW   -> shadowDispatcher;
            case LIVE     -> liveDispatcher;
            case DISABLED -> null;
        };
    }
}
