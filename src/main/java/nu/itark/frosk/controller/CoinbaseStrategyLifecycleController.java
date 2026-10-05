package nu.itark.frosk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.lifecycle.CoinbaseStrategyLifecycleService;
import nu.itark.frosk.crypto.coinbase.lifecycle.CoinbaseStrategyLifecycleService.InvalidTransitionException;
import nu.itark.frosk.crypto.coinbase.lifecycle.CoinbaseStrategyLifecycleService.PromotionRefusedException;
import nu.itark.frosk.crypto.coinbase.lifecycle.CoinbaseStrategyLifecycleService.StrategyNotFoundException;
import nu.itark.frosk.crypto.coinbase.lifecycle.CoinbaseStrategyReadinessDTO;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Strategy lifecycle for the crypto (Coinbase) process (port 8081): readiness
 * metrics and PAPER → SHADOW → LIVE promotion. Mirrors
 * {@code KrakenStrategyLifecycleController} exactly, on its own URL prefix and
 * backed by {@link CoinbaseStrategyLifecycleService}.
 *
 * <p>Status codes: 404 unknown strategy, 409 transition not allowed from the
 * current mode (or bad targetMode), 422 promotion criteria not met — the 422
 * body is the full readiness object, so {@code blockers} says exactly why.
 */
@RestController
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class CoinbaseStrategyLifecycleController {

    private final CoinbaseStrategyLifecycleService lifecycleService;

    /** Body of promote/demote: {@code {"targetMode": "SHADOW"}}. */
    public record ModeChangeRequest(StrategyMode targetMode) {}

    /**
     * Every registered strategy — one call for the dashboard panel.
     *
     * @Example GET http://localhost:8081/coinbase/strategy/readiness
     */
    @GetMapping("/coinbase/strategy/readiness")
    public List<CoinbaseStrategyReadinessDTO> readinessAll() {
        return lifecycleService.readinessAll();
    }

    /**
     * @Example GET http://localhost:8081/coinbase/strategy/CryptoEMACrossLongIntradayStrategy/readiness
     */
    @GetMapping("/coinbase/strategy/{strategyName}/readiness")
    public CoinbaseStrategyReadinessDTO readiness(@PathVariable String strategyName) {
        return lifecycleService.readiness(strategyName);
    }

    /**
     * @Example POST http://localhost:8081/coinbase/strategy/CryptoEMACrossLongIntradayStrategy/promote {"targetMode":"SHADOW"}
     */
    @PostMapping("/coinbase/strategy/{strategyName}/promote")
    public CoinbaseStrategyReadinessDTO promote(@PathVariable String strategyName, @RequestBody ModeChangeRequest body) {
        log.warn("POST /coinbase/strategy/{}/promote targetMode={}", strategyName, body.targetMode());
        return lifecycleService.promote(strategyName, body.targetMode());
    }

    /**
     * Rollback — PAPER or DISABLED, never gated.
     *
     * @Example POST http://localhost:8081/coinbase/strategy/CryptoEMACrossLongIntradayStrategy/demote {"targetMode":"PAPER"}
     */
    @PostMapping("/coinbase/strategy/{strategyName}/demote")
    public CoinbaseStrategyReadinessDTO demote(@PathVariable String strategyName, @RequestBody ModeChangeRequest body) {
        log.warn("POST /coinbase/strategy/{}/demote targetMode={}", strategyName, body.targetMode());
        return lifecycleService.demote(strategyName, body.targetMode());
    }

    // ── error mapping ────────────────────────────────────────────────────

    @ExceptionHandler(PromotionRefusedException.class)
    public ResponseEntity<CoinbaseStrategyReadinessDTO> refused(PromotionRefusedException e) {
        return ResponseEntity.unprocessableEntity().body(e.getReadiness());
    }

    @ExceptionHandler(StrategyNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(StrategyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
