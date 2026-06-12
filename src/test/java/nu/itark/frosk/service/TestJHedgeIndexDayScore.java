package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.HedgeIndex;
import nu.itark.frosk.repo.HedgeIndexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link HedgeIndexService#getScore} returns the regime level in
 * effect on a given day: indicator states are carried forward from their last
 * event, and lookups floor to the calendar day so intraday (15m) timestamps
 * and {@code ZonedDateTime.now()} resolve correctly.
 *
 * <p>Uses far-future event dates and asserts deltas relative to a controlled
 * baseline so the persistent test data is left untouched.
 */
public class TestJHedgeIndexDayScore extends BaseIntegrationTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Autowired
    HedgeIndexService hedgeIndexService;

    @Autowired
    HedgeIndexRepository hedgeIndexRepository;

    private final List<HedgeIndex> created = new ArrayList<>();

    @Test
    public void scoreCarriesStateForwardAndFloorsToDay() {
        ZonedDateTime day0 = ZonedDateTime.of(2030, 1, 7, 0, 0, 0, 0, STOCKHOLM);
        try {
            // Pin VIX and VSTOXX to risk-on so the baseline is controlled,
            // regardless of what state the persistent test data left them in.
            saveEvent("^VIX", day0, false);
            saveEvent("^V2TX", day0, false);
            int base = hedgeIndexService.getScore(day0);

            // Day 1: VIX flips risk-off
            saveEvent("^VIX", day0.plusDays(1), true);
            assertEquals(base + 1, hedgeIndexService.getScore(day0.plusDays(1)));
            // Day 2 has no event — state carried forward
            assertEquals(base + 1, hedgeIndexService.getScore(day0.plusDays(2)));

            // Day 3: VSTOXX also risk-off → +1 indicator +1 cluster bonus
            saveEvent("^V2TX", day0.plusDays(3), true);
            assertEquals(base + 3, hedgeIndexService.getScore(day0.plusDays(3)));
            // Intraday timestamp on day 4 floors to the day (carried state)
            assertEquals(base + 3, hedgeIndexService.getScore(day0.plusDays(4).withHour(10).withMinute(15)));

            // Day 5: VIX back to risk-on → only VSTOXX remains, no cluster
            saveEvent("^VIX", day0.plusDays(5), false);
            assertEquals(base + 1, hedgeIndexService.getScore(day0.plusDays(5)));
            // Far future still carries the latest state
            assertEquals(base + 1, hedgeIndexService.getScore(day0.plusDays(30)));
        } finally {
            hedgeIndexRepository.deleteAll(created);
            hedgeIndexService.clearCache();
        }
    }

    private void saveEvent(String indicator, ZonedDateTime date, boolean risk) {
        HedgeIndex hi = new HedgeIndex();
        hi.setDate(Date.from(date.toInstant()));
        hi.setIndicator(indicator);
        hi.setCategory("Test");
        hi.setRuleDesc("test event");
        hi.setRisk(risk);
        hi.setPrice(BigDecimal.ONE);
        created.add(hedgeIndexRepository.save(hi));
        hedgeIndexService.clearCache();
    }
}
