package nu.itark.frosk.controller;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.service.CandleBackfillService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Research backfill of 15m candles to CSV (crypto process, port 8081). See
 * {@link CandleBackfillService}.
 */
@RestController
@Profile("crypto")
@RequiredArgsConstructor
public class CandleBackfillController {

    private final CandleBackfillService backfillService;

    /** @Example POST http://localhost:8081/crypto/backfill/candles?days=400 */
    @PostMapping("/crypto/backfill/candles")
    public ResponseEntity<Map<String, Object>> start(@RequestParam(defaultValue = "400") int days) {
        if (days < 1 || days > 800) {
            return ResponseEntity.badRequest().body(Map.of("error", "days must be 1..800"));
        }
        boolean started = backfillService.start(days);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("started", started);
        body.put("outDir", backfillService.getOutDir());
        return ResponseEntity.status(started ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT).body(body);
    }

    /** @Example GET http://localhost:8081/crypto/backfill/candles/status */
    @GetMapping("/crypto/backfill/candles/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("running", backfillService.isRunning());
        body.put("products", backfillService.getStatus());
        return body;
    }
}
