package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** One security in the S2 multi-factor composite z-score screen. */
@Data
@Builder
public class MultiFactorCandidateDTO {
    private String securityName;
    private String securityDesc;
    private double momentum12mPct;
    /** Raw value factor (1/priceToBook, or EPS/price earnings-yield fallback). Higher = cheaper. */
    private double valueRaw;
    private String valueSource;
    /** Raw quality factor (-debtToEquity, or -beta fallback). Higher = better quality. */
    private double qualityRaw;
    private String qualitySource;
    private double zMomentum;
    private double zValue;
    private double zQuality;
    /** Weighted composite z-score (0.4 mom + 0.3 value + 0.3 quality). */
    private double composite;
}
