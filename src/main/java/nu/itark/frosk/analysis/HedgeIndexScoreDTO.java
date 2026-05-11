package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HedgeIndexScoreDTO {
    private String date;
    private int score;
    private String regime;
}
