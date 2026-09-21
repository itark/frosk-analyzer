package nu.itark.frosk.regime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body from the frosk-garch-service {@code POST /volatility-regime}
 * endpoint. {@code volatilityRegime} is one of "LOW" / "NORMAL" / "HIGH" /
 * "UNKNOWN" (the Python service's fail-safe value) — not the frosk-analyzer
 * {@link Regime} enum, which additionally folds in the ADX trend axis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VolatilityRegimeResponse(
        @JsonProperty("conditional_volatility") double conditionalVolatility,
        @JsonProperty("volatility_percentile") double volatilityPercentile,
        @JsonProperty("volatility_regime") String volatilityRegime,
        @JsonProperty("converged") boolean converged,
        @JsonProperty("n_observations") int nObservations,
        @JsonProperty("model") String model
) {
}
