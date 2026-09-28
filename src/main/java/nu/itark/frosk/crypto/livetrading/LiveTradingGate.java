package nu.itark.frosk.crypto.livetrading;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.repo.LiveOrderRepository;
import nu.itark.frosk.strategies.SignalStrength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Guards all live order placement and sizes new positions off account equity.
 *
 * <p>Checks performed by {@link #canTrade(String, BigDecimal)}:
 * <ol>
 *   <li>Master kill switch ({@code crypto.live.trading.enabled})</li>
 *   <li>Total open exposure cap ({@code crypto.live.trading.max.total.exposure.pct} of equity)</li>
 *   <li>Per-order position size cap</li>
 *   <li>Daily realized-loss limit (auto-disables when breached)</li>
 *   <li>Available EUR balance (buy-only)</li>
 * </ol>
 *
 * <p>Toggle via {@code POST /api/crypto/live-trading/enable|disable}.
 */
@Service
@Profile({"crypto", "kraken-futures"})
@Slf4j
public class LiveTradingGate {

    @Value("${crypto.live.trading.enabled:false}")
    private boolean enabled;

    @Value("${crypto.live.trading.position.pct.of.equity:0.05}")
    private BigDecimal positionPctOfEquity;

    @Value("${crypto.live.trading.position.min.eur:25}")
    private BigDecimal minPositionEur;

    @Value("${crypto.live.trading.max.position.eur:500}")
    private BigDecimal maxPositionEur;

    @Value("${crypto.live.trading.max.daily.loss.eur:2000}")
    private BigDecimal maxDailyLossEur;

    @Value("${crypto.live.trading.max.total.exposure.pct:0.5}")
    private BigDecimal maxTotalExposurePct;

    /**
     * Live-side mirror of {@code RiskManagementService.enabled} for paper — gates
     * only the max-open-positions check below, added 2026-09-28 to close a
     * config-parity gap where that circuit breaker existed for paper but not live.
     * Shares the exact property key so the two can never drift apart. Does not
     * affect any of this class's pre-existing checks (exposure cap, position-size
     * cap, daily-loss kill switch, balance check), which have always been
     * unconditional.
     */
    @Value("${risk.enabled:true}")
    private boolean riskEnabled;

    /**
     * Max simultaneous open positions across all strategies/symbols. Shares the
     * exact key RiskManagementService reads for paper (risk.max.open.positions)
     * so the two limits cannot silently diverge.
     */
    @Value("${risk.max.open.positions:5}")
    private int maxOpenPositions;

    /** Mirrors {@code CryptoPaperTradingService}'s multipliers — kept in sync deliberately. */
    @Value("${crypto.signal.strength.elevated.multiplier:1.5}")
    private BigDecimal elevatedMultiplier;

    @Value("${crypto.signal.strength.strong.multiplier:2.0}")
    private BigDecimal strongMultiplier;

    @Autowired
    private LiveOrderRepository liveOrderRepository;

    @Autowired
    private BrokerOrderClient brokerOrderClient;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.warn("LiveTradingGate: live trading manually set to {}", enabled);
    }

    /**
     * Equity used for position sizing: EUR cash on hand plus the cost basis of
     * currently open positions (unrealized PnL is not marked to market here —
     * realized PnL already flows back into the EUR balance once a position closes).
     */
    public BigDecimal computeEquity() {
        BigDecimal cash = brokerOrderClient.getAvailableBalance();
        BigDecimal openExposure = liveOrderRepository.sumOpenExposureEur();
        return cash.add(openExposure);
    }

    /**
     * Position size for a new entry: {@code equity * positionPctOfEquity}, clamped
     * to {@code [minPositionEur, maxPositionEur]} so sizing scales with account
     * growth/drawdown but never drops below an exchange-viable order size or
     * above the absolute per-trade ceiling.
     */
    public BigDecimal computePositionSizeEur() {
        return computePositionSizeEur(null);
    }

    /**
     * As above, scaled by the rule-based confidence tier of the signal that
     * triggered this entry. {@code null} (or {@link SignalStrength#BASE}) applies
     * no scaling. The multiplier is applied before clamping, so a strong signal
     * can reach the cap sooner but never exceed {@code maxPositionEur}.
     */
    public BigDecimal computePositionSizeEur(SignalStrength strength) {
        BigDecimal equity = computeEquity();
        BigDecimal raw = equity.multiply(positionPctOfEquity).multiply(multiplierFor(strength));
        return raw.max(minPositionEur).min(maxPositionEur);
    }

    private BigDecimal multiplierFor(SignalStrength strength) {
        if (strength == null) return BigDecimal.ONE;
        return switch (strength) {
            case STRONG -> strongMultiplier;
            case ELEVATED -> elevatedMultiplier;
            case BASE -> BigDecimal.ONE;
        };
    }

    /**
     * Returns true if it is safe to place a new order.
     *
     * @param ticker    product ID (e.g. "BTC-EUR")
     * @param eurAmount how much EUR the order would spend
     */
    public boolean canTrade(String ticker, BigDecimal eurAmount) {
        if (!enabled) {
            log.debug("LiveTradingGate: canTrade=false — master switch is OFF");
            return false;
        }
        BigDecimal equity = computeEquity();
        BigDecimal openExposure = liveOrderRepository.sumOpenExposureEur();
        BigDecimal maxTotalExposure = equity.multiply(maxTotalExposurePct);
        if (openExposure.add(eurAmount).compareTo(maxTotalExposure) > 0) {
            log.warn("LiveTradingGate: canTrade=false — open exposure {} + new order {} would exceed {} "
                    + "({}% of equity {})", openExposure, eurAmount, maxTotalExposure, maxTotalExposurePct, equity);
            return false;
        }
        if (eurAmount.compareTo(maxPositionEur) > 0) {
            log.warn("LiveTradingGate: canTrade=false — eurAmount {} exceeds max {}", eurAmount, maxPositionEur);
            return false;
        }
        if (riskEnabled && maxOpenPositionsReached()) {
            return false;
        }
        if (dailyLossExceeded()) {
            setEnabled(false);
            return false;
        }
        BigDecimal eurBalance = brokerOrderClient.getAvailableBalance();
        if (eurBalance.compareTo(eurAmount) < 0) {
            log.warn("LiveTradingGate: canTrade=false — EUR balance {} < order amount {}", eurBalance, eurAmount);
            return false;
        }
        return true;
    }

    /**
     * Live mirror of RiskManagementService's paper-only max-open-positions check
     * (added 2026-09-28). Counts entries that are, or may be, open — same
     * definition {@link #computeEquity()} and the exposure cap above already use.
     */
    boolean maxOpenPositionsReached() {
        long openCount = liveOrderRepository.countOpenPositions();
        if (openCount >= maxOpenPositions) {
            log.warn("LiveTradingGate: canTrade=false — max open positions reached ({}/{})",
                    openCount, maxOpenPositions);
            return true;
        }
        return false;
    }

    boolean dailyLossExceeded() {
        LocalDateTime midnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        BigDecimal loss = liveOrderRepository.sumEurLossSince(midnight);
        if (loss == null) return false;
        // loss is negative; compare absolute value to limit
        if (loss.abs().compareTo(maxDailyLossEur) >= 0) {
            log.warn("KILL SWITCH: daily loss limit reached (loss={}EUR, limit={}EUR)", loss.abs(), maxDailyLossEur);
            return true;
        }
        return false;
    }

    public BigDecimal todayPnl() {
        LocalDateTime midnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        BigDecimal pnl = liveOrderRepository.sumRealizedPnlSince(midnight);
        return pnl != null ? pnl : BigDecimal.ZERO;
    }

    public long todayOrderCount() {
        LocalDateTime midnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        return liveOrderRepository.countByCreatedAtAfter(midnight);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void logHourlyStatus() {
        log.info("LIVE TRADING STATUS: enabled={} | today: {} orders | realized: {}EUR | daily_loss_limit: {}EUR",
                enabled, todayOrderCount(), todayPnl(), maxDailyLossEur);
    }
}
