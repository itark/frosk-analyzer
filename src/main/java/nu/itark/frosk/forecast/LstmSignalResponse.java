package nu.itark.frosk.forecast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body from the LSTM signal-filter service {@code POST /signal-filter}.
 * {@code fallback=true} means the service itself degraded to a safe default
 * rather than a genuine model prediction — {@link nu.itark.frosk.service.LstmSignalFilterService}
 * treats that the same as no data at all (fail closed), it does not act on
 * a fallback prediction even if it happens to say "BUY".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LstmSignalResponse(
        @JsonProperty("signal_probability") double signalProbability,
        String prediction,
        boolean fallback
) {
}
