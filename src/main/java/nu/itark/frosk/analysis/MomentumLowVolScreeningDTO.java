package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Result of the S1 momentum-first low-vol double screen (docs/research/omx-equity-strategies-research.md). */
@Data
@Builder
public class MomentumLowVolScreeningDTO {
    private String date;
    private int universeSize;
    /** Momentum (12m %) at the top-fraction cutoff — securities at/above this cleared the first screen. */
    private double momentumCutoffPct;
    /** Universe median volatility (%) — the second screen keeps securities below this. */
    private double medianVolatility;
    private int maxPositions;
    private int selectedCount;
    private List<MomentumLowVolCandidateDTO> selected;
}
