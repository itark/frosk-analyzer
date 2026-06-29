package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/**
 * Latest-session price move for a single security, used by the Performance
 * dashboard's "Today's Winners" list.
 */
@Data
@Builder
public class TopWinnerDTO {
    private String name;
    private String description;
    private double close;
    private double previousClose;
    private double change;
    private double changePercent;
    private String date;
}
