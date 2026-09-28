package nu.itark.frosk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.InvalidTransitionException;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.PromotionRefusedException;
import nu.itark.frosk.crypto.kraken.lifecycle.KrakenStrategyLifecycleService.StrategyNotFoundException;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyReadinessDTO;
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
 * Strategy lifecycle for the kraken-futures process (port 8082): readiness
 * metrics and PAPER → SHADOW → LIVE promotion. See
 * {@link KrakenStrategyLifecycleService} for the rules.
 *
 * <p>Status codes: 404 unknown strategy, 409 transition not allowed from the
 * current mode (or bad targetMode), 422 promotion criteria not met — the 422
 * body is the full readiness object, so {@code blockers} says exactly why.
 */
@RestController
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenStrategyLifecycleController {

    private final KrakenStrategyLifecycleService lifecycleService;

    /** Body of promote/demote: {@code {"targetMode": "SHADOW"}}. */
    public record ModeChangeRequest(StrategyMode targetMode) {}

    /**
     * Every registered strategy — one call for the dashboard panel.
     *
     * @Example GET http://localhost:8082/kraken/strategy/readiness
     */
    @GetMapping("/kraken/strategy/readiness")
    public List<StrategyReadinessDTO> readinessAll() {
        return lifecycleService.readinessAll();
    }

    /**
     * @Example GET http://localhost:8082/kraken/strategy/CryptoShortIntradayStrategy/readiness
     */
    @GetMapping("/kraken/strategy/{strategyName}/readiness")
    public StrategyReadinessDTO readiness(@PathVariable String strategyName) {
        return lifecycleService.readiness(strategyName);
    }

    /**
     * @Example POST http://localhost:8082/kraken/strategy/CryptoShortIntradayStrategy/promote {"targetMode":"SHADOW"}
     */
    @PostMapping("/kraken/strategy/{strategyName}/promote")
    public StrategyReadinessDTO promote(@PathVariable String strategyName, @RequestBody ModeChangeRequest body) {
        log.warn("POST /kraken/strategy/{}/promote targetMode={}", strategyName, body.targetMode());
        return lifecycleService.promote(strategyName, body.targetMode());
    }

    /**
     * Rollback — PAPER or DISABLED, never gated.
     *
     * @Example POST http://localhost:8082/kraken/strategy/CryptoShortIntradayStrategy/demote {"targetMode":"PAPER"}
     */
    @PostMapping("/kraken/strategy/{strategyName}/demote")
    public StrategyReadinessDTO demote(@PathVariable String strategyName, @RequestBody ModeChangeRequest body) {
        log.warn("POST /kraken/strategy/{}/demote targetMode={}", strategyName, body.targetMode());
        return lifecycleService.demote(strategyName, body.targetMode());
    }

    // ── error mapping ────────────────────────────────────────────────────

    @ExceptionHandler(PromotionRefusedException.class)
    public ResponseEntity<StrategyReadinessDTO> refused(PromotionRefusedException e) {
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
