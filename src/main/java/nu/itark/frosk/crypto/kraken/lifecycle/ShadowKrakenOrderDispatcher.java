package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Sends the order to BOTH venues: a real Kraken order and a paper position,
 * each tagged {@code SHADOW} ({@code live_order.execution_mode} /
 * {@code kraken_futures_paper_order.execution_mode}) so the two fills for the
 * same signal can be paired for slippage/fee comparison.
 *
 * <p>Paper goes first and the venues are isolated from each other: a failure
 * placing the real order must not cost the paper record (the comparison
 * baseline), and a paper failure must not stop a real order.
 */
@Component
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class ShadowKrakenOrderDispatcher implements KrakenOrderDispatcher {

    private final PaperKrakenOrderDispatcher paperDispatcher;
    private final LiveKrakenOrderDispatcher liveDispatcher;

    @Override
    public void dispatch(OrderRequest order) {
        log.info("SHADOW ORDER: {} {} {} @ {} — sending to Kraken AND paper (execution_mode=SHADOW)",
                order.strategyName(), order.signalType(), order.ticker(), order.closePrice());
        try {
            paperDispatcher.dispatch(order, StrategyMode.SHADOW);
        } catch (Exception e) {
            log.error("SHADOW ORDER: paper leg failed for {} {} {}", order.strategyName(), order.signalType(),
                    order.ticker(), e);
        }
        try {
            liveDispatcher.dispatch(order, StrategyMode.SHADOW);
        } catch (Exception e) {
            log.error("SHADOW ORDER: live leg failed for {} {} {}", order.strategyName(), order.signalType(),
                    order.ticker(), e);
        }
    }
}
