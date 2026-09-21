package nu.itark.frosk.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One daily datapoint for the BTC Trend Follower chart in the frosk-dashboard
 * crypto card. A pure function of the persisted daily BTC-EUR price series —
 * the same EMA/ADX values {@link nu.itark.frosk.service.CryptoBtcTrendSignalService}
 * derives the LONG/SHORT/NEUTRAL signal from, exposed here as a time series so
 * the card can plot price against its two EMAs and the ADX trend filter.
 *
 * @param date   the daily bar's date (UTC)
 * @param close  BTC-EUR close
 * @param ema10  fast EMA at that close
 * @param ema20  slow EMA at that close
 * @param adx    ADX(14) at that close
 * @param signal the LONG/SHORT/NEUTRAL state at that bar (EMA fast vs slow,
 *               gated by ADX &gt; threshold) — lets the dashboard mark every
 *               signal change and score each LONG/SHORT run
 */
public record BtcTrendChartPoint(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
        BigDecimal close,
        BigDecimal ema10,
        BigDecimal ema20,
        BigDecimal adx,
        String signal
) {
}
