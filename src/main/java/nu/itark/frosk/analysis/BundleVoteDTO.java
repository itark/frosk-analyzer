package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * One security's score in the S7 bundle-screening ensemble: how many of the independent
 * frosk strategies currently hold an open position in it, and which ones.
 */
@Data
@Builder
public class BundleVoteDTO {
    private String securityName;
    private String securityDesc;
    private int votes;
    private List<String> strategies;
}
