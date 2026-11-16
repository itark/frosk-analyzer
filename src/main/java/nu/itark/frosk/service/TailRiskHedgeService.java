package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.TailRiskHedgeDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * S11 — Tail-risk hedging for the OMX portfolio (docs/research/omx-equity-strategies-research.md).
 * Maps the HedgeIndex tier to a graded short-ETP (XACT Bear) hedge ratio: 0% risk-on, scaling up
 * as the regime deteriorates. Read-only — produces a target ratio, places no orders.
 *
 * <p>Note: XACT Bear carries high daily carry (contango) — intended as short-term protection,
 * not a buy-and-hold hedge.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TailRiskHedgeService {

    private final HedgeIndexService hedgeIndexService;

    @Value("${frosk.s11.hedge.instrument:XACT-BEAR.ST}")
    private String hedgeInstrument;

    @Value("${frosk.s11.hedge.max.pct:10.0}")
    private double maxHedgePct;

    public TailRiskHedgeDTO hedge() {
        int score = hedgeIndexService.getScore(ZonedDateTime.now());

        double ratio;
        if (score <= 3) {
            ratio = 0.0;                 // risk-on: no hedge
        } else if (score <= 7) {
            ratio = maxHedgePct * 0.3;   // cautious
        } else if (score <= 11) {
            ratio = maxHedgePct * 0.6;   // defensive
        } else {
            ratio = maxHedgePct;         // deep risk-off: max hedge
        }
        ratio = Math.round(ratio * 100.0) / 100.0;

        String guidance = ratio == 0
                ? "Risk-on — no hedge."
                : String.format("Allocate %.1f%% to %s as a short-term tail-risk hedge (watch the daily carry).",
                        ratio, hedgeInstrument);

        log.info("S11 tail-risk hedge: score={}, ratio={}%", score, ratio);

        return TailRiskHedgeDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .hedgeIndexScore(score)
                .regime(HedgeIndexService.regimeLabel(score))
                .hedgeInstrument(hedgeInstrument)
                .hedgeRatioPct(ratio)
                .guidance(guidance)
                .build();
    }
}
