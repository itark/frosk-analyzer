package nu.itark.frosk.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for frosk-prophet-service {@code POST /daily-pnl-forecast}.
 * Field names must match that Python service's {@code schemas.DailyPnlForecastRequest} exactly.
 */
public record DailyPnlForecastRequest(
        @JsonProperty("strategy_name") String strategyName,
        @JsonProperty("daily_pnl") List<DailyPnlPoint> dailyPnl,
        @JsonProperty("horizon_days") int horizonDays
) {
}
