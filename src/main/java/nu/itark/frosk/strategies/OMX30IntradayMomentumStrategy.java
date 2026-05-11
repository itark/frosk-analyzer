package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * OMX30 Intraday Momentum Strategy — Tier-0 (10-minute ticker, 5-minute bars)
 *
 * <p>This strategy runs on 5-minute bars of the {@code ^OMX} (OMXS30) index.
 * It is evaluated every 10 minutes during Stockholm market hours
 * (09:00–17:30 CET, Monday–Friday) by the {@link nu.itark.frosk.service.IntradayStrategyRunner}.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>EMA(9) &gt; EMA(21) — short-term uptrend confirmed on 5m bars
 *       (9 bars ≈ 45 min trend; 21 bars ≈ 105 min / ~1.75 h)</li>
 *   <li>RSI(7) crosses above 45 — momentum turning up from neutral/oversold</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>RSI(7) &gt; 70 — overbought, take profit</li>
 *   <li>EMA(9) crosses below EMA(21) — short-term trend reversal</li>
 *   <li>Max 6 bars held (30 minutes) — intraday time-based exit</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>No volume-based rules: {@code ^OMX} is an index — volume data is
 *       often zero or unavailable at the intraday level from Yahoo Finance.</li>
 *   <li>The strategy is intentionally simple so it back-tests cleanly on a
 *       shallow 7-day bar window and produces low-noise signals.</li>
 *   <li>This is a signal-generation layer, not automated execution.
 *       The output drives the {@code intraday_signal} table which a human
 *       trader monitors before placing orders.</li>
 * </ul>
 */
@Component
@Slf4j
public class OMX30IntradayMomentumStrategy extends AbstractStrategy implements IIndicatorValue {

    // ── Indicator parameters ────────────────────────────────────────────────
    private static final int    EMA_FAST        = 9;   // 45 min
    private static final int    EMA_SLOW        = 21;  // 105 min (~1.75 h)
    private static final int    RSI_PERIOD      = 7;
    private static final double RSI_ENTRY_LEVEL = 45.0;
    private static final double RSI_EXIT_LEVEL  = 70.0;
    private static final int    MAX_BARS_HELD   = 6;   // 30 min

    // ── Cached indicator refs (populated in buildStrategy) ──────────────────
    private EMAIndicator cachedEmaFast;
    private EMAIndicator cachedEmaSlow;
    private RSIIndicator cachedRsi;

    // -----------------------------------------------------------------------

    /**
     * Build the ta4j {@link Strategy} for the provided 5-minute {@link BarSeries}.
     *
     * <p>Safe to call repeatedly — each call rebuilds indicators and clears
     * the {@code indicatorValues} snapshot list.
     */
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        cachedEmaFast = new EMAIndicator(close, EMA_FAST);
        cachedEmaSlow = new EMAIndicator(close, EMA_SLOW);
        cachedRsi     = new RSIIndicator(close, RSI_PERIOD);

        setIndicatorValues(close,         "close");
        setIndicatorValues(cachedEmaFast, "ema9");
        setIndicatorValues(cachedEmaSlow, "ema21");
        setIndicatorValues(cachedRsi,     "rsi7");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule uptrend   = new OverIndicatorRule(cachedEmaFast, cachedEmaSlow);
        Rule rsiEntry  = new CrossedUpIndicatorRule(cachedRsi, RSI_ENTRY_LEVEL);
        Rule entryRule = uptrend.and(rsiEntry);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule rsiOverbought  = new OverIndicatorRule(cachedRsi, RSI_EXIT_LEVEL);
        Rule trendReversal  = new CrossedDownIndicatorRule(cachedEmaFast, cachedEmaSlow);
        Rule timeExit       = new MaxBarsHeldRule(MAX_BARS_HELD);
        Rule exitRule       = rsiOverbought.or(trendReversal).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Convenience: read EMA(9) at {@code index} from the most recently built series.
     * Returns {@code null} if {@link #buildStrategy(BarSeries)} has not been called.
     */
    public Double getEma9At(int index) {
        return cachedEmaFast != null ? cachedEmaFast.getValue(index).doubleValue() : null;
    }

    /** Convenience: read EMA(21) at {@code index}. */
    public Double getEma21At(int index) {
        return cachedEmaSlow != null ? cachedEmaSlow.getValue(index).doubleValue() : null;
    }

    /** Convenience: read RSI(7) at {@code index}. */
    public Double getRsi7At(int index) {
        return cachedRsi != null ? cachedRsi.getValue(index).doubleValue() : null;
    }

    // ── Inner rule: time-based exit ─────────────────────────────────────────

    private static class MaxBarsHeldRule extends AbstractRule {

        private final int maxBars;

        MaxBarsHeldRule(int maxBars) {
            this.maxBars = maxBars;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
                return false;
            }
            int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
            return (index - entryIndex) >= maxBars;
        }
    }
}
