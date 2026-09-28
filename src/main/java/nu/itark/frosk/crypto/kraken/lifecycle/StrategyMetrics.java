package nu.itark.frosk.crypto.kraken.lifecycle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Performance metrics over a strategy's closed trades, in close order.
 *
 * <p>Every metric works on the per-trade return {@code r = netPnl / notional}
 * rather than on dollar PnL. Paper positions are sized at a few percent of a
 * collateral pool shared by all strategies, so a dollar drawdown measured
 * against that pool would be tiny for any single strategy and the 15% ceiling
 * could never bind. Per-trade returns describe the strategy itself,
 * independent of how much capital it happened to be allocated.
 *
 * <ul>
 *   <li><b>maxDrawdownPct</b> — largest peak-to-trough fall of the compounded
 *       equity curve {@code Π(1 + r)}, i.e. as if the strategy traded its whole
 *       allocation on every signal. The conservative reading for a promotion gate.</li>
 *   <li><b>sharpeRatio</b> — mean(r) / sample stdev(r), per trade, not annualised
 *       (trade frequency varies too much across strategies for an annualisation
 *       factor to mean anything). Null below two trades or with zero variance.</li>
 *   <li><b>winRate</b> — share of trades with net PnL &gt; 0.</li>
 *   <li><b>avgRR</b> — mean winning r / |mean losing r|. Null without both a win and a loss.</li>
 * </ul>
 */
public record StrategyMetrics(
        int closedTrades,
        BigDecimal maxDrawdownPct,
        BigDecimal sharpeRatio,
        BigDecimal winRate,
        BigDecimal avgRR,
        BigDecimal totalPnlUsd) {

    /** One closed trade: net PnL after fees and funding, and the notional it was opened with. */
    public record Trade(BigDecimal netPnlUsd, BigDecimal notionalUsd) {}

    public static StrategyMetrics of(List<Trade> trades) {
        int n = 0;
        double[] returns = new double[trades.size()];
        BigDecimal totalPnl = BigDecimal.ZERO;
        for (Trade t : trades) {
            if (t.netPnlUsd() == null || t.notionalUsd() == null || t.notionalUsd().signum() == 0) continue;
            returns[n++] = t.netPnlUsd().doubleValue() / t.notionalUsd().doubleValue();
            totalPnl = totalPnl.add(t.netPnlUsd());
        }
        if (n == 0) {
            return new StrategyMetrics(0, null, null, null, null, BigDecimal.ZERO);
        }

        double equity = 1.0, peak = 1.0, maxDd = 0.0;
        double sum = 0, winSum = 0, lossSum = 0;
        int wins = 0, losses = 0;
        for (int i = 0; i < n; i++) {
            double r = returns[i];
            equity *= (1.0 + r);
            peak = Math.max(peak, equity);
            maxDd = Math.max(maxDd, (peak - equity) / peak);
            sum += r;
            if (r > 0) { wins++; winSum += r; }
            else if (r < 0) { losses++; lossSum += r; }
        }

        double mean = sum / n;
        BigDecimal sharpe = null;
        if (n >= 2) {
            double var = 0;
            for (int i = 0; i < n; i++) var += (returns[i] - mean) * (returns[i] - mean);
            double std = Math.sqrt(var / (n - 1));
            if (std > 0) sharpe = scale(mean / std, 2);
        }
        BigDecimal avgRR = (wins > 0 && losses > 0)
                ? scale((winSum / wins) / Math.abs(lossSum / losses), 2)
                : null;

        return new StrategyMetrics(
                n,
                scale(maxDd * 100.0, 2),
                sharpe,
                scale((double) wins / n, 4),
                avgRR,
                totalPnl.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal scale(double v, int places) {
        return BigDecimal.valueOf(v).setScale(places, RoundingMode.HALF_UP);
    }
}
