package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fractional-Kelly position fraction per strategy, from that strategy's rolling
 * paper track record (its last {@value #WINDOW_TRADES} closed trades in
 * {@code crypto_paper_order}).
 *
 * <p>{@code f* = W - (1 - W) / R}, where W = share of winning trades and
 * R = average win / average loss. Returned fraction = {@value #KELLY_SCALE} × f*,
 * clamped to [{@value #MIN_FRACTION}, {@value #MAX_FRACTION}] of equity; below
 * {@code crypto.kelly.min.trades} closed trades the strategy gets
 * {@value #FALLBACK_FRACTION} instead.
 *
 * <p>Each trade is measured as a return on its own entry notional
 * ({@code realizedPnlEur / entryEurAmount}), not in EUR, so the sizes this
 * service chooses cannot feed back into its own W/R estimate. Rows without an
 * entry notional (pre-2026-09 OrderFlowImbalance) are skipped.
 *
 * <p>Read by both {@link CryptoPaperTradingService} (paper) and
 * {@code LiveTradingGate} (live) under the same {@code crypto.kelly.sizing.enabled}
 * key, so paper and live size identically. Live deliberately uses the paper
 * track record: it is the longest unbroken record of the same signals.
 */
@Service
@Slf4j
public class KellyPositionSizingService {

    static final int WINDOW_TRADES = 100;
    static final double KELLY_SCALE = 0.25;
    static final double MIN_FRACTION = 0.005;
    static final double MAX_FRACTION = 0.05;
    static final double FALLBACK_FRACTION = 0.02;

    @Value("${crypto.kelly.sizing.enabled:false}")
    private boolean enabled;

    @Value("${crypto.kelly.min.trades:20}")
    private int minTrades;

    @Autowired
    private CryptoPaperOrderRepository orderRepository;

    public boolean isEnabled() {
        return enabled;
    }

    /** Fraction of equity to allocate to a new entry of {@code strategyName}. */
    public BigDecimal kellyFraction(String strategyName) {
        List<CryptoPaperOrder> closed = orderRepository
                .findTop100ByStrategyNameAndSideOrderByCreatedAtDesc(strategyName, "SELL");
        int n = 0, wins = 0;
        double winSum = 0, lossSum = 0;
        for (CryptoPaperOrder o : closed) {
            if (o.getRealizedPnlEur() == null || o.getEntryEurAmount() == null
                    || o.getEntryEurAmount().signum() <= 0) {
                continue;
            }
            double ret = o.getRealizedPnlEur().doubleValue() / o.getEntryEurAmount().doubleValue();
            n++;
            if (ret > 0) {
                wins++;
                winSum += ret;
            } else {
                lossSum += -ret;
            }
        }
        if (n < minTrades) {
            log.debug("Kelly: {} has {} closed trades (< {}) — fallback {}", strategyName, n, minTrades, FALLBACK_FRACTION);
            return BigDecimal.valueOf(FALLBACK_FRACTION);
        }
        double fraction = clamp(KELLY_SCALE * kelly(wins, n, winSum, lossSum));
        log.debug("Kelly: {} n={} W={} fraction={}", strategyName, n, (double) wins / n, fraction);
        return BigDecimal.valueOf(fraction);
    }

    /** Full Kelly f* from win count and summed win/loss returns (losses as positive magnitudes). */
    static double kelly(int wins, int n, double winSum, double lossSum) {
        int losses = n - wins;
        if (wins == 0) return -1.0;                 // nothing but losers: as small as allowed
        if (losses == 0 || lossSum == 0) return 1.0; // no measurable loss: as large as allowed
        double w = (double) wins / n;
        double r = (winSum / wins) / (lossSum / losses);
        return w - (1 - w) / r;
    }

    private static double clamp(double f) {
        return Math.max(MIN_FRACTION, Math.min(MAX_FRACTION, f));
    }
}
