package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** S4 two-layer market filter on OMXS30: TSMOM (252d return) + SMA-200, with a vol-regime scaler. */
@Data
@Builder
public class MarketRegimeDTO {
    private String date;
    private String index;
    private double tsmom252Pct;
    private boolean tsmomBull;
    private double closeVsSma200Pct;
    private boolean sma200Bull;
    /** Current 20d realized vol relative to its 252d median. */
    private double volRatio;
    private boolean volElevated;
    /** Target equity exposure 0.0 / 0.5 / 1.0 (halved again when vol is elevated). */
    private double exposure;
    private String regime;
}
