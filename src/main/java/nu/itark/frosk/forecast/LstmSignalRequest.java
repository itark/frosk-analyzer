package nu.itark.frosk.forecast;

import java.util.List;

/** Request body for the LSTM signal-filter service {@code POST /signal-filter}. */
public record LstmSignalRequest(
        String ticker,
        List<LstmBar> bars
) {
}
