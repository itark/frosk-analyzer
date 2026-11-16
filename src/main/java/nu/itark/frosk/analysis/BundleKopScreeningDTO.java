package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of the S6 BundleKöp ensemble screen — the integrated 4-factor composite (momentum,
 * value, quality, low-vol) gated by the S4 market regime. See
 * {@code docs/research/omx-equity-strategies-research.md}.
 */
@Data
@Builder
public class BundleKopScreeningDTO {
    private String date;
    private int universeSize;
    /** When false (TSMOM/SMA-200 not both bullish), no new buys are made — selection is empty. */
    private boolean marketBull;
    private String marketRegime;
    private double weightMomentum;
    private double weightValue;
    private double weightQuality;
    private double weightLowVol;
    private int maxPositions;
    private int selectedCount;
    private List<BundleKopCandidateDTO> selected;
}
