package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.kraken.lifecycle.OrderRequest;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import nu.itark.frosk.service.CryptoPaperTradingService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Simulated fill against the Coinbase paper account — the pre-existing paper
 * behaviour, unchanged apart from the execution-mode tag on new positions.
 * Mirrors {@code PaperKrakenOrderDispatcher}. Coinbase is long-only (no SHRT/COVR).
 */
@Component
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class PaperCoinbaseOrderDispatcher implements CoinbaseOrderDispatcher {

    private final CryptoPaperTradingService paperTradingService;

    @Override
    public void dispatch(OrderRequest order) {
        dispatch(order, StrategyMode.PAPER);
    }

    /** As {@link #dispatch(OrderRequest)}, tagging new positions with {@code mode} (PAPER or SHADOW). */
    void dispatch(OrderRequest order, StrategyMode mode) {
        switch (order.signalType()) {
            case "BUY"  -> paperTradingService.dispatchBuy(order.strategyName(), order.ticker(),
                    order.closePrice(), order.strength(), mode.name());
            case "SELL" -> paperTradingService.dispatchSell(order.strategyName(), order.ticker(), order.closePrice());
            default     -> log.warn("PaperCoinbaseOrderDispatcher: unknown signal type '{}'", order.signalType());
        }
    }
}
