package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** S11 tail-risk hedge: HedgeIndex tier -> graded short-ETP (XACT Bear) hedge ratio. */
@Data
@Builder
public class TailRiskHedgeDTO {
    private String date;
    private int hedgeIndexScore;
    private String regime;
    private String hedgeInstrument;
    /** Target share of portfolio in the short hedge (0% risk-on -> max when deeply risk-off). */
    private double hedgeRatioPct;
    private String guidance;
}
