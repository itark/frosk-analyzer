package nu.itark.frosk.forecast;

/** One day's aggregated realized PnL for a strategy. Sent to frosk-prophet-service. */
public record DailyPnlPoint(
        String date,
        double value
) {
}
