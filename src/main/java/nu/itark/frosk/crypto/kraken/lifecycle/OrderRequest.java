package nu.itark.frosk.crypto.kraken.lifecycle;

import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.strategies.SignalStrength;

import java.math.BigDecimal;

/**
 * One intraday signal on its way to a venue.
 *
 * @param signalType   BUY / SELL (long) or SHRT / COVR (short)
 * @param closePrice   bar close at signal time — the paper fill price and the
 *                     live fallback fill price
 * @param signal       the persisted signal row, flagged {@code live} once a real
 *                     order fills; null for synthetic exits (orphan reconcile)
 * @param strength     entry sizing tier; null on exits
 */
public record OrderRequest(
        String signalType,
        String strategyName,
        String ticker,
        BigDecimal closePrice,
        IntradaySignal signal,
        SignalStrength strength) {

    public boolean isEntry() {
        return "BUY".equals(signalType) || "SHRT".equals(signalType);
    }
}
