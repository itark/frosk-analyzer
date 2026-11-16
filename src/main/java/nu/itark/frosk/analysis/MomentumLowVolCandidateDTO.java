package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** One security in the S1 momentum-first low-vol screen. */
@Data
@Builder
public class MomentumLowVolCandidateDTO {
    private String securityName;
    private String securityDesc;
    /** 12-month return, percent (ROC over the momentum lookback). */
    private double momentum12mPct;
    /** Daily log-return stdev over the vol lookback, percent. */
    private double volatility;
    /** True when the security passed both screens (top momentum tertile AND below-median vol). */
    private boolean selected;
}
