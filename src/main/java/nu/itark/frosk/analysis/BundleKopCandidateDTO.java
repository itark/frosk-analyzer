package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** One security in the S6 BundleKöp 4-factor ensemble (momentum + value + quality + low-vol). */
@Data
@Builder
public class BundleKopCandidateDTO {
    private String securityName;
    private String securityDesc;
    private double momentum12mPct;
    private double valueRaw;
    private double qualityRaw;
    private double volatility;
    private double zMomentum;
    private double zValue;
    private double zQuality;
    private double zLowVol;
    /** Weighted composite (0.3 mom + 0.2 value + 0.2 quality + 0.3 low-vol). */
    private double composite;
}
