package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** S5 turn-of-month timing: active on the last 2 + first 3 trading days of the month. */
@Data
@Builder
public class TurnOfMonthDTO {
    private String date;
    private boolean inWindow;
    private String guidance;
}
