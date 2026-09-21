package nu.itark.frosk.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One forecasted day from frosk-prophet-service. */
public record ForecastPoint(
        String date,
        double yhat,
        @JsonProperty("yhat_lower") double yhatLower,
        @JsonProperty("yhat_upper") double yhatUpper
) {
}
