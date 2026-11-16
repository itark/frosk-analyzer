package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** S8 Nordic calendar anomalies: payday-shifted turn-of-month + reporting-month pre-drift window. */
@Data
@Builder
public class NordicCalendarDTO {
    private String date;
    /** Swedish payday-shifted ToM window (~27th–4th). */
    private boolean paydayTomWindow;
    /** OMX reporting month (Jan/Apr/Jul/Oct) — pre-report drift tends to be positive. */
    private boolean reportMonth;
    private String guidance;
}
