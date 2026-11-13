package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Result of the S2 integrated multi-factor composite z-score screen. */
@Data
@Builder
public class MultiFactorScreeningDTO {
    private String date;
    /** Securities for which all three factors were computable (the intersection set). */
    private int universeSize;
    private double weightMomentum;
    private double weightValue;
    private double weightQuality;
    private int maxPositions;
    private int selectedCount;
    private List<MultiFactorCandidateDTO> selected;
}
