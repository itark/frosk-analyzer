package nu.itark.frosk.forecast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body from frosk-prophet-service {@code POST /daily-pnl-forecast}.
 * {@code dataPointsUsed} is not in the original contract sketch but is part
 * of the Python service's real response — kept here so callers can show it
 * alongside {@code sufficientData} rather than losing it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProphetForecastResponse(
        List<ForecastPoint> forecast,
        @JsonProperty("horizon_days") int horizonDays,
        @JsonProperty("data_points_used") int dataPointsUsed,
        @JsonProperty("sufficient_data") boolean sufficientData
) {
}
