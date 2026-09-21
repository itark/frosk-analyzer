package nu.itark.frosk.regime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for the frosk-garch-service {@code POST /volatility-regime}
 * endpoint. Field names must match {@code schemas.VolatilityRegimeRequest}
 * in that Python service exactly.
 */
public record VolatilityRegimeRequest(
        @JsonProperty("ticker") String ticker,
        @JsonProperty("returns") List<Double> returns
) {
}
