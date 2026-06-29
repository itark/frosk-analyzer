package nu.itark.frosk.api;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.service.CoinbaseOrderClient;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST interface for toggling live trading and inspecting its status.
 * Only active on the {@code crypto} Spring profile (port 8081).
 */
@RestController
@RequestMapping("/api/crypto/live-trading")
@Profile("crypto")
@Slf4j
public class LiveTradingController {

    @Autowired
    private LiveTradingGate liveTradingGate;

    @Autowired
    private CoinbaseOrderClient coinbaseOrderClient;

    /** Current status: master switch + today's realized PnL. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "enabled", liveTradingGate.isEnabled(),
                "dailyPnlEur", liveTradingGate.todayPnl(),
                "todayOrders", liveTradingGate.todayOrderCount()
        );
    }

    /** Enable live trading (runtime toggle — does not persist across restarts). */
    @PostMapping("/enable")
    public Map<String, Object> enable() {
        liveTradingGate.setEnabled(true);
        log.warn("LiveTradingController: live trading ENABLED via REST");
        return Map.of("enabled", true);
    }

    /** Disable live trading immediately. */
    @PostMapping("/disable")
    public Map<String, Object> disable() {
        liveTradingGate.setEnabled(false);
        log.warn("LiveTradingController: live trading DISABLED via REST");
        return Map.of("enabled", false);
    }

    /** Available balances for all non-zero accounts. */
    @GetMapping("/balance")
    public Map<String, Object> balance() {
        Map<String, BigDecimal> balances = coinbaseOrderClient.getAllBalances();
        BigDecimal eurBalance = balances.getOrDefault("EUR", BigDecimal.ZERO);
        return Map.of(
                "eurBalance", eurBalance,
                "balances", balances
        );
    }
}
