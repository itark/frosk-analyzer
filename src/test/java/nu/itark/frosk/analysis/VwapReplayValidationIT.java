package nu.itark.frosk.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Same replay as {@link VwapReplayIT} but with the configuration that was live when the recorded
 * signals (June – 2026-10-05) were produced: no trend filter and no loss cooldown (both were
 * enabled on 2026-10-06/07). Used ONLY by {@link VwapFilterAnalysisIT#replayMatchesRecordedSignals}
 * to prove the replay reproduces what the strategy actually did.
 *
 * <pre>
 *   mvn test -Dtest=VwapReplayValidationIT#replay -Dfrosk.replay.truncateVolume=true \
 *       -Dfrosk.replay.out=~/itark/backfill/replay_trades_validation.csv
 * </pre>
 */
@TestPropertySource(properties = {
        "crypto.vwap.trend.filter.enabled=false",
        "crypto.vwap.cooldown.bars.after.loss=0"
})
public class VwapReplayValidationIT extends VwapReplayIT {

    /** Deliberately differs from production (see class javadoc) — the parity guard does not apply. */
    @Override
    @Test
    public void productionParametersAreMirrored() {
        // no-op
    }
}
