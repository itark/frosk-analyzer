package nu.itark.frosk.forecast;

/** One OHLCV bar sent to the LSTM signal-filter service. */
public record LstmBar(
        double open,
        double high,
        double low,
        double close,
        double volume
) {
}
