package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of the S7 bundle-screening ("jury system") ensemble — see
 * {@code docs/research/omx-equity-strategies-research.md}.
 *
 * <p>Candidates are securities that at least {@code minVotes} of the independent bundle
 * strategies agree on. {@code vetoed} reflects the HedgeIndex gate: when risk-off, no buys
 * should be made regardless of votes — the candidates are still listed for transparency.
 */
@Data
@Builder
public class BundleScreeningDTO {
    private String date;
    private int hedgeIndexScore;
    private String regime;
    private boolean vetoed;
    private int minVotes;
    private List<String> bundleStrategies;
    private List<BundleVoteDTO> candidates;
}
