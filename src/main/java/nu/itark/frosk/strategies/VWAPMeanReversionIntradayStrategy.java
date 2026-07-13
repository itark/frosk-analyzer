package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.IntradayBarCountIndicator;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * VWAP Mean Reversion — "Gummibandshandeln"
 *
 * <p>Prices gravitate back to VWAP (Volume Weighted Average Price) during the day.
 * OMX30 stocks with high liquidity (VOLV-B, ERIC-B, SEB-A) show this pattern clearly.
 * Market makers and algorithmic trading force price back to fair value — VWAP is their
 * reference point.
 *
 * <p>Uses a true session-anchored VWAP ({@link SessionVWAPIndicator}) that resets at
 * each day's open. The previous SMA(20) proxy drifted across the day, which broke both
 * the entry distance and the reversion target.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Price stretched at least 0.15% below session VWAP (covers the 0.06% round-trip
 *       fee more than twice before the reversion target)</li>
 *   <li>RSI(5) &lt; 30 — short-term oversold on pullback</li>
 *   <li>EMA(20) &gt; EMA(40) — day is in an uptrend (only buy pullbacks in uptrends)</li>
 *   <li>Bar count 4–26 — only trade 10:00–15:30 (avoid open/close volatility)</li>
 *   <li>HedgeIndex score &le; frosk.intraday.hedge.max.score (default 9) — no entries in
 *       strong risk-off, where stretched prices keep stretching</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: price back above session VWAP (the mean-reversion thesis)</li>
 *   <li>RSI(5) &gt; 65 — momentum exhaustion</li>
 *   <li>Stop: 0.6% below entry (≈2× the entry stretch)</li>
 *   <li>Max 12 bars held (~3 hours)</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <p>This strategy requires volume data to work well, so it runs on individual
 * OMX30 stocks (which have volume), NOT on the ^OMX index.
 */
@Component
@Slf4j
public class VWAPMeanReversionIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    // ── Parameters ─────────────────────────────────────────────────────────
    private static final double VWAP_STRETCH      = 0.9985; // Entry at >=0.15% below VWAP
    private static final int    EMA_FAST          = 20;  // Trend filter fast
    private static final int    EMA_SLOW          = 40;  // Trend filter slow
    private static final int    RSI_PERIOD        = 5;   // Fast RSI
    private static final double RSI_ENTRY_LEVEL   = 30.0; // Oversold threshold
    private static final double RSI_EXIT_LEVEL    = 65.0; // Momentum exhaustion
    private static final int    MIN_BAR_FOR_ENTRY = 4;   // ~10:00 (after opening chop)
    private static final int    MAX_BAR_FOR_ENTRY = 26;  // ~15:30 (before close)
    private static final double STOP_LOSS_PCT     = 0.6; // Hard stop below entry
    private static final int    MAX_BARS_HELD     = 12;  // ~3 hours
    @Autowired
    private HedgeIndexService hedgeIndexService;

    /** Block entries when the HedgeIndex score exceeds this (9 = strong risk-off only). */
    @org.springframework.beans.factory.annotation.Value("${frosk.intraday.hedge.max.score:9}")
    private int intradayHedgeMaxScore;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SessionVWAPIndicator vwap = new SessionVWAPIndicator(series);
        TransformIndicator vwapStretched = TransformIndicator.multiply(vwap, VWAP_STRETCH);
        EMAIndicator emaFast = new EMAIndicator(close, EMA_FAST);
        EMAIndicator emaSlow = new EMAIndicator(close, EMA_SLOW);
        RSIIndicator rsi = new RSIIndicator(close, RSI_PERIOD);
        IntradayBarCountIndicator barCount = new IntradayBarCountIndicator(series);

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "sessionVwap");
        setIndicatorValues(emaFast, "ema20");
        setIndicatorValues(emaSlow, "ema40");
        setIndicatorValues(rsi, "rsi5");

        // ── Entry ─────────────────────────────────────────────────────────
        // Price stretched below session VWAP (mean-reversion pullback)
        Rule pullbackToMean = new UnderIndicatorRule(close, vwapStretched);
        // RSI oversold on pullback
        Rule rsiOversold = new UnderIndicatorRule(rsi, DoubleNum.valueOf(RSI_ENTRY_LEVEL));
        // Uptrend context: EMA(20) > EMA(40)
        Rule uptrend = new OverIndicatorRule(emaFast, emaSlow);
        // Time filter: bar 4–26 (10:00–15:30)
        Rule timeOk = new OverIndicatorRule(barCount, DoubleNum.valueOf(MIN_BAR_FOR_ENTRY - 1))
                .and(new UnderIndicatorRule(barCount, DoubleNum.valueOf(MAX_BAR_FOR_ENTRY + 1)));
        // Macro regime gate
        Rule riskOn = new HedgeIndexMaxScoreRule(series, hedgeIndexService, intradayHedgeMaxScore);

        Rule entryRule = pullbackToMean.and(rsiOversold).and(uptrend).and(timeOk).and(riskOn);

        // ── Exit ──────────────────────────────────────────────────────────
        // Profit target: price back above session VWAP
        Rule profitTarget = new OverIndicatorRule(close, vwap);
        // RSI momentum exhaustion
        Rule rsiExhaustion = new OverIndicatorRule(rsi, DoubleNum.valueOf(RSI_EXIT_LEVEL));
        // Hard stop
        Rule stopLoss = new StopLossRule(close, STOP_LOSS_PCT);
        // Time exit
        Rule timeExit = new MaxBarsHeldRule(MAX_BARS_HELD);

        Rule exitRule = profitTarget.or(rsiExhaustion).or(stopLoss).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
