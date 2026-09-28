package nu.itark.frosk.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.broker.BrokerOrderClient;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.repo.LiveOrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime kill switch for real-money trading on the kraken-futures process
 * (port 8082) — the {@code crypto.live.trading.enabled} master switch in
 * {@link LiveTradingGate}, which every SHADOW/LIVE entry must pass.
 *
 * <ul>
 *   <li>{@code GET  /kraken/live-trading/status}</li>
 *   <li>{@code POST /kraken/live-trading/disable} — no body, never refused. Stopping
 *       must always be one call.</li>
 *   <li>{@code POST /kraken/live-trading/enable} — requires the JSON body
 *       {@code {"confirm": "ENABLE_LIVE_TRADING"}}.</li>
 * </ul>
 *
 * <p><b>Disable stops new entries only.</b> Exits are deliberately not gated, so
 * open positions are still closed by their exit signals — cutting those off
 * would leave real positions unmanaged.
 *
 * <p><b>Why enable needs a body.</b> The app's CORS config allows every origin,
 * so a body-less POST could be fired at localhost by any web page the user has
 * open (a plain HTML form needs no preflight). A JSON body forces a preflight
 * and cannot come from a form. It is not authentication: a page that runs
 * script can still send it while CORS allows every origin.
 *
 * <p><b>Enable is refused while an order is UNRESOLVED</b>: its state on Kraken is
 * unknown, and resuming trading on top of a possibly-open position the system
 * does not track is exactly how exposure limits get silently exceeded.
 *
 * <p>The switch is in memory. A restart always comes back OFF (the property
 * value) — the fail-safe direction for a kill switch.
 */
@RestController
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenLiveTradingController {

    static final String CONFIRM_PHRASE = "ENABLE_LIVE_TRADING";

    private final LiveTradingGate liveTradingGate;
    private final BrokerOrderClient brokerOrderClient;
    private final LiveOrderRepository liveOrderRepository;

    public record EnableRequest(String confirm) {}

    /**
     * @Example GET http://localhost:8082/kraken/live-trading/status
     */
    @GetMapping("/kraken/live-trading/status")
    public Map<String, Object> status() {
        return statusBody();
    }

    /**
     * @Example POST http://localhost:8082/kraken/live-trading/disable
     */
    @PostMapping("/kraken/live-trading/disable")
    public Map<String, Object> disable(HttpServletRequest request) {
        liveTradingGate.setEnabled(false);
        log.warn("KILL SWITCH: Kraken live trading DISABLED via REST (from {})", request.getRemoteAddr());
        return statusBody();
    }

    /**
     * @Example POST http://localhost:8082/kraken/live-trading/enable {"confirm":"ENABLE_LIVE_TRADING"}
     */
    @PostMapping(value = "/kraken/live-trading/enable", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> enable(@RequestBody(required = false) EnableRequest body,
                                                      HttpServletRequest request) {
        if (body == null || !CONFIRM_PHRASE.equals(body.confirm())) {
            return refuse(HttpStatus.BAD_REQUEST,
                    "Bekräftelse saknas — skicka {\"confirm\": \"" + CONFIRM_PHRASE + "\"}");
        }
        long unresolved = liveOrderRepository.countByStatus("UNRESOLVED");
        if (unresolved > 0) {
            return refuse(HttpStatus.CONFLICT, unresolved + " order(s) är UNRESOLVED — deras läge på Kraken är okänt. "
                    + "Kontrollera på futures.kraken.com och reda ut dem i live_order innan live trading slås på.");
        }
        liveTradingGate.setEnabled(true);
        log.warn("KILL SWITCH: Kraken live trading ENABLED via REST (from {})", request.getRemoteAddr());
        return ResponseEntity.ok(statusBody());
    }

    private ResponseEntity<Map<String, Object>> refuse(HttpStatus status, String reason) {
        log.warn("KILL SWITCH: enable refused — {}", reason);
        Map<String, Object> body = statusBody();
        body.put("error", reason);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> statusBody() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", liveTradingGate.isEnabled());
        m.put("availableMarginUsd", brokerOrderClient.getAvailableBalance());
        m.put("openExposureUsd", liveOrderRepository.sumOpenExposureEur());
        m.put("todayRealizedPnlUsd", liveTradingGate.todayPnl());
        m.put("todayOrders", liveTradingGate.todayOrderCount());
        m.put("pendingOrders", liveOrderRepository.countByStatus("PENDING"));
        m.put("unresolvedOrders", liveOrderRepository.countByStatus("UNRESOLVED"));
        return m;
    }
}
