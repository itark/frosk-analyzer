package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.EquityFactorScreeningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke/report harness for the S1 and S2 equity factor screens — runs both against the
 * seeded test DB and prints the selections. Read-only; nothing is written. Run with:
 * {@code mvn test -Dmaven.test.skip=false -Dtest=EquityFactorScreeningIT}.
 */
public class EquityFactorScreeningIT extends BaseIntegrationTest {

    @Autowired
    EquityFactorScreeningService screeningService;

    @Test
    public void reportMomentumLowVol() {
        MomentumLowVolScreeningDTO s1 = screeningService.screenMomentumLowVol();
        assertNotNull(s1);
        System.out.printf("%n=== S1 momentum-lowvol: universe=%d, momentumCutoff=%.2f%%, medianVol=%.3f%%, selected=%d ===%n",
                s1.getUniverseSize(), s1.getMomentumCutoffPct(), s1.getMedianVolatility(), s1.getSelectedCount());
        s1.getSelected().stream().limit(15).forEach(c ->
                System.out.printf("  %-14s mom=%8.2f%%  vol=%6.3f%%  %s%n",
                        c.getSecurityName(), c.getMomentum12mPct(), c.getVolatility(),
                        c.getSecurityDesc() == null ? "" : c.getSecurityDesc()));
    }

    @Test
    public void reportMultiFactor() {
        MultiFactorScreeningDTO s2 = screeningService.screenMultiFactorComposite();
        assertNotNull(s2);
        System.out.printf("%n=== S2 multi-factor: universe=%d, weights m/v/q=%.1f/%.1f/%.1f, selected=%d ===%n",
                s2.getUniverseSize(), s2.getWeightMomentum(), s2.getWeightValue(), s2.getWeightQuality(), s2.getSelectedCount());
        s2.getSelected().stream().limit(15).forEach(c ->
                System.out.printf("  %-14s composite=%6.2f  (zMom=%5.2f zVal=%5.2f zQual=%5.2f)  val=%s qual=%s%n",
                        c.getSecurityName(), c.getComposite(), c.getZMomentum(), c.getZValue(), c.getZQuality(),
                        c.getValueSource(), c.getQualitySource()));
    }
}
