package nu.itark.frosk.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily BTC-EUR trend signal for the frosk-dashboard crypto card. A pure
 * function of the persisted daily BTC-EUR price series (see
 * {@link nu.itark.frosk.service.CryptoBtcTrendSignalService}), recomputed on
 * every request — nothing here is stored.
 *
 * @param signal            "LONG" / "SHORT" / "NEUTRAL"
 * @param avanzaInstrument   what to hold on Avanza for that signal
 * @param ema10              fast EMA at the last daily close
 * @param ema20              slow EMA at the last daily close
 * @param adx                ADX(14) at the last daily close
 * @param adxThreshold       the ADX floor below which the signal is NEUTRAL
 * @param daysSinceSignalChange  trading days since the signal last differed
 * @param lastSignalChangeDate   date the current signal first took effect
 * @param asOfDate           date of the last daily bar the signal was computed from
 * @param dataAvailable      false when there is not enough BTC-EUR history yet
 */
public record BtcTrendSignal(
        String signal,
        String avanzaInstrument,
        BigDecimal ema10,
        BigDecimal ema20,
        BigDecimal adx,
        double adxThreshold,
        int daysSinceSignalChange,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate lastSignalChangeDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate asOfDate,
        boolean dataAvailable
) {

    public static final String LONG = "LONG";
    public static final String SHORT = "SHORT";
    public static final String NEUTRAL = "NEUTRAL";

    public static String avanzaInstrumentFor(String signal) {
        return switch (signal) {
            case LONG -> "BULL BITCOIN X2 AVA";
            case SHORT -> "BEAR BITCOIN X2 AVA";
            default -> "Håll kassa";
        };
    }

    public static BtcTrendSignal unavailable(double adxThreshold, LocalDate asOfDate) {
        return new BtcTrendSignal(NEUTRAL, avanzaInstrumentFor(NEUTRAL),
                null, null, null, adxThreshold, 0, null, asOfDate, false);
    }
}
