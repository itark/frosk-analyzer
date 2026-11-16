package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** S3 Halloween seasonal overlay with momentum confirmation (invested Nov–Apr if OMX momentum > 0). */
@Data
@Builder
public class SeasonalTimingDTO {
    private String date;
    private boolean halloweenSeason;
    private double omxMomentum12mPct;
    private boolean momentumPositive;
    /** Invested when in the Halloween season AND 12m OMX momentum is positive. */
    private boolean invested;
    private String guidance;
}
