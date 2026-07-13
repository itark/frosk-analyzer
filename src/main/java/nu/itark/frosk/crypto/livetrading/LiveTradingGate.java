package nu.itark.frosk.crypto.livetrading;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseOrderClient;
import nu.itark.frosk.repo.LiveOrderRepository;
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
@Profile("crypto")
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

    @Autowired
    private LiveOrderRepository liveOrderRepository;

    @Autowired
    private CoinbaseOrderClient coinbaseOrderClient;

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
        BigDecimal cash = coinbaseOrderClient.getEurBalance();
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
        BigDecimal equity = computeEquity();
        BigDecimal raw = equity.multiply(positionPctOfEquity);
        return raw.max(minPositionEur).min(maxPositionEur);
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
        if (dailyLossExceeded()) {
            setEnabled(false);
            return false;
        }
        BigDecimal eurBalance = coinbaseOrderClient.getEurBalance();
        if (eurBalance.compareTo(eurAmount) < 0) {
            log.warn("LiveTradingGate: canTrade=false — EUR balance {} < order amount {}", eurBalance, eurAmount);
            return false;
        }
        return true;
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
