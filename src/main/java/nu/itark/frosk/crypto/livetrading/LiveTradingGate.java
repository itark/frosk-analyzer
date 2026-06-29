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
 * Guards all live order placement behind three checks:
 * <ol>
 *   <li>Master kill switch ({@code crypto.live.trading.enabled})</li>
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

    @Value("${crypto.live.trading.max.position.eur:500}")
    private BigDecimal maxPositionEur;

    @Value("${crypto.live.trading.max.daily.loss.eur:2000}")
    private BigDecimal maxDailyLossEur;

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
