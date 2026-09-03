package nu.itark.frosk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CryptoPaperAccountDTO;
import nu.itark.frosk.service.KrakenFuturesPaperTradingService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard endpoints for the {@code kraken-futures} process (port 8082).
 *
 * <p>Mirrors the paths served by {@link CryptoDashboardController} on the
 * {@code crypto} process so the frosk-dashboard "Paper Account" card works
 * unchanged when its source toggle points at Kraken Futures. Without this
 * controller {@code GET /crypto/paper-account} 404s on 8082 and the card is
 * stuck in its "Loading…" state.
 */
@RestController
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenFuturesDashboardController {

    private final KrakenFuturesPaperTradingService krakenFuturesPaperTradingService;

    /**
     * @Example GET http://localhost:8082/crypto/paper-account
     */
    @GetMapping(value = "/crypto/paper-account")
    public CryptoPaperAccountDTO getPaperAccount() {
        log.info("GET /crypto/paper-account (kraken-futures)");
        return krakenFuturesPaperTradingService.getAccountSummary();
    }
}
