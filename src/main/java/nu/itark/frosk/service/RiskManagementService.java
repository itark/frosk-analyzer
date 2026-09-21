package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Basic real-money-shaped risk framework for Kraken Futures paper trading — gates
 * every entry signal in {@link KrakenFuturesPaperTradingService#openPosition}
 * through three independent checks:
 *
 * <ol>
 *   <li><b>Daily loss circuit breaker</b> — if today's (UTC) realized PnL drops
 *       below {@code risk.daily.max.loss.usd}, all new entries are blocked until
 *       the next UTC day. Checked (and re-evaluated) on every {@link #checkEntry}
 *       call, so it trips as soon as a query reflects the breach — no separate
 *       "check after every close" hook is needed, and a stale flag can never
 *       outlive its day (auto-resets the moment the UTC date rolls over).</li>
 *   <li><b>Position size limit</b> — a single position may not exceed {@code
 *       risk.max.position.pct} of total account value (collateral + open
 *       exposure, the same figure the caller already sizes positions against).</li>
 *   <li><b>Max open positions</b> — blocks new entries once {@code
 *       risk.max.open.positions} positions are already OPEN.</li>
 * </ol>
 *
 * <p><b>Disabled contract:</b> {@code risk.enabled=false} makes {@link #checkEntry}
 * always return {@link RiskCheckResult#allow()} with no DB queries — same
 * "feature flag off must never silently block" shape as every other gate in this
 * codebase (c.f. {@code AbstractStrategy.liquidityRule()}, {@code
 * RegimeForecastService}), just inverted: here disabled means never blocking,
 * because this gate's job is to add a safety net, not a trading signal.
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class RiskManagementService {

    @Value("${risk.enabled:true}")
    private boolean enabled;

    @Value("${risk.daily.max.loss.usd:-500}")
    private BigDecimal dailyMaxLossUsd;

    @Value("${risk.max.position.pct:0.10}")
    private BigDecimal maxPositionPct;

    @Value("${risk.max.open.positions:5}")
    private int maxOpenPositions;

    private final KrakenFuturesPaperOrderRepository orderRepository;

    /** UTC day the circuit breaker last tripped for, or null if not currently halted. */
    private volatile LocalDate haltedOnDay;

    public RiskManagementService(KrakenFuturesPaperOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * True while the daily-loss circuit breaker is tripped for today (UTC).
     * Always false when {@code risk.enabled=false} — no DB query either, same
     * "fully inert when disabled" contract as {@link #checkEntry}.
     */
    public boolean isTradingHalted() {
        if (!enabled) {
            return false;
        }
        refreshCircuitBreaker();
        return haltedOnDay != null;
    }

    /**
     * Risk check for a proposed new position. Callers pass the position size
     * they are about to open and the account value they sized it against
     * ({@code null} skips the position-size check — it cannot be evaluated
     * without a denominator).
     */
    public RiskCheckResult checkEntry(BigDecimal proposedPositionUsd, BigDecimal accountValueUsd) {
        if (!enabled) {
            return RiskCheckResult.allow();
        }

        refreshCircuitBreaker();
        if (haltedOnDay != null) {
            return RiskCheckResult.blocked(
                    "Trading halted — daily realized loss breached " + dailyMaxLossUsd + "USD today (UTC)");
        }

        if (accountValueUsd != null && accountValueUsd.signum() > 0) {
            BigDecimal maxPositionUsd = accountValueUsd.multiply(maxPositionPct);
            if (proposedPositionUsd.compareTo(maxPositionUsd) > 0) {
                String reason = "Position size " + proposedPositionUsd + "USD exceeds " + maxPositionPct
                        + " of account value " + accountValueUsd + "USD (max " + maxPositionUsd + "USD)";
                log.warn("RiskManagementService: {}", reason);
                return RiskCheckResult.blocked(reason);
            }
        }

        long openCount = orderRepository.countByStatus("OPEN");
        if (openCount >= maxOpenPositions) {
            String reason = "Max open positions reached (" + openCount + "/" + maxOpenPositions + ")";
            log.warn("RiskManagementService: {}", reason);
            return RiskCheckResult.blocked(reason);
        }

        return RiskCheckResult.allow();
    }

    /**
     * Re-evaluates the circuit breaker against today's (UTC) realized PnL.
     * Auto-resets {@code haltedOnDay} the moment the UTC date has moved past
     * the day it was tripped for — the breaker cannot outlive its own day.
     */
    private synchronized void refreshCircuitBreaker() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (haltedOnDay != null && !haltedOnDay.equals(today)) {
            log.info("RiskManagementService: new UTC day ({}) — circuit breaker reset", today);
            haltedOnDay = null;
        }

        BigDecimal realizedToday = orderRepository.sumRealizedPnlUsdSince(today.atStartOfDay());
        if (realizedToday.compareTo(dailyMaxLossUsd) < 0) {
            if (haltedOnDay == null) {
                log.warn("RiskManagementService: CIRCUIT BREAKER TRIPPED — realized PnL today {}USD < limit {}USD. "
                        + "Trading halted until the next UTC day.", realizedToday, dailyMaxLossUsd);
            }
            haltedOnDay = today;
        }
    }
}
