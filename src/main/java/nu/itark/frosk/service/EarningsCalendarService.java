package nu.itark.frosk.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.YahooFinanceDirectClient;
import nu.itark.frosk.model.EarningsSnapshot;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.EarningsSnapshotRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Captures, once per day per security, everything knowable in advance about the next
 * earnings report: expected date, whether that date is confirmed, and the analyst
 * consensus EPS with its dispersion.
 *
 * <p><b>Why this exists as an append-only log.</b> Yahoo serves only the CURRENT
 * expectation and overwrites it silently as estimates move. There is no historical
 * earnings calendar and no consensus-revision history available retroactively from
 * any endpoint this system can reach. The revision series — which is the input with
 * genuine documented predictive content for announcement returns — exists only if it
 * is captured day by day, starting now. A day not captured is lost permanently.
 *
 * <p><b>Never update a row in place.</b> Doing so would reproduce the defect that
 * made {@code security.trailingEps} and {@code security.yoyGrowth} unusable: single
 * current-value columns with no date dimension, read by backtests as though the
 * present value had been knowable in the past.
 *
 * <p>This service only collects. It makes no prediction and implies no strategy.
 */
@Service
@Slf4j
public class EarningsCalendarService {

    @Autowired
    private YahooFinanceDirectClient yahooClient;

    @Autowired
    private EarningsSnapshotRepository snapshotRepository;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private SecurityPriceRepository securityPriceRepository;

    @Value("${frosk.earnings.capture.enabled:true}")
    private boolean enabled;

    /** Politeness delay between tickers — same rationale as yahoo.fetch.delay.ms. */
    @Value("${yahoo.fetch.delay.ms:300}")
    private long fetchDelayMs;

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    /**
     * One capture pass over active Swedish equities. Idempotent per day: a security
     * already captured today is skipped, so re-running is safe and a partial failure
     * can simply be re-run.
     */
    public void captureAll() {
        if (!enabled) {
            log.info("EarningsCalendarService: disabled (frosk.earnings.capture.enabled=false)");
            return;
        }
        LocalDate today = LocalDate.now(STOCKHOLM);
        List<Security> securities = securityRepository.findByDatabaseAndActive("YAHOO", true).stream()
                .filter(s -> s.getName() != null && s.getName().endsWith(".ST"))
                .toList();

        int captured = 0, skipped = 0, failed = 0;
        for (Security s : securities) {
            if (snapshotRepository.existsBySecurityIdAndCapturedOn(s.getId(), today)) {
                skipped++;
                continue;
            }
            try {
                if (capture(s, today)) {
                    captured++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("EarningsCalendarService: capture failed for {} — {}", s.getName(), e.toString());
            }
            sleep();
        }
        log.info("EarningsCalendarService: captured={}, skipped(already today)={}, no-data={} of {} .ST securities",
                captured, skipped, failed, securities.size());
    }

    private boolean capture(Security s, LocalDate today) {
        JsonNode cal = yahooClient.getModuleCalendarEvents(s.getName());
        if (cal == null || cal.isMissingNode()) {
            return false;
        }
        JsonNode earnings = cal.path("earnings");
        if (earnings.isMissingNode()) {
            return false;
        }

        EarningsSnapshot snap = new EarningsSnapshot();
        snap.setSecurityId(s.getId());
        snap.setTicker(s.getName());
        snap.setCapturedAt(Instant.now());
        snap.setCapturedOn(today);
        snap.setAnnouncementDate(firstDate(earnings.path("earningsDate")));
        if (earnings.hasNonNull("isEarningsDateEstimate")) {
            snap.setDateIsEstimate(earnings.path("isEarningsDateEstimate").asBoolean());
        }
        snap.setEpsConsensus(raw(earnings.path("earningsAverage")));
        snap.setEpsLow(raw(earnings.path("earningsLow")));
        snap.setEpsHigh(raw(earnings.path("earningsHigh")));
        snap.setRevenueConsensus(raw(earnings.path("revenueAverage")));
        snap.setClosePrice(latestClose(s));

        // A row with no date AND no consensus carries nothing worth storing.
        if (snap.getAnnouncementDate() == null && snap.getEpsConsensus() == null) {
            return false;
        }
        snapshotRepository.save(snap);
        return true;
    }

    /** Yahoo wraps numbers as {"raw": x, "fmt": "..."} and omits the node when absent. */
    private BigDecimal raw(JsonNode node) {
        JsonNode r = node.path("raw");
        return r.isNumber() ? BigDecimal.valueOf(r.asDouble()) : null;
    }

    private LocalDate firstDate(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return null;
        }
        JsonNode r = arrayNode.get(0).path("raw");
        if (!r.isNumber()) {
            return null;
        }
        return Instant.ofEpochSecond(r.asLong()).atZone(STOCKHOLM).toLocalDate();
    }

    private BigDecimal latestClose(Security s) {
        try {
            return securityPriceRepository.findBySecurityIdOrderByTimestamp(s.getId()).stream()
                    .reduce((a, b) -> b)
                    .map(p -> p.getClose())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void sleep() {
        if (fetchDelayMs <= 0) return;
        try {
            Thread.sleep(fetchDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
