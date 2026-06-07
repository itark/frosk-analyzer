package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.indicators.IntradayBarCountIndicator;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.*;

import java.util.List;

/**
 * VWAP Mean Reversion — "Gummibandshandeln"
 *
 * <p>Prices gravitate back to VWAP (Volume Weighted Average Price) during the day.
 * OMX30 stocks with high liquidity (VOLV-B, ERIC-B, SEB-A) show this pattern clearly.
 * Market makers and algorithmic trading force price back to fair value — VWAP is their
 * reference point.
 *
 * <p>Since ta4j 0.16 does not have a built-in intraday VWAP that resets daily, we
 * approximate with a 20-period SMA as the mean-reversion target (effectively a
 * volume-unweighted VWAP proxy on 15m bars — ~5 hours lookback). On OMX30 large-caps,
 * the SMA(20) on 15m bars closely tracks VWAP due to relatively uniform volume distribution.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Price pulls back to or below the SMA(20) "VWAP proxy"</li>
 *   <li>RSI(5) &lt; 35 — short-term oversold on pullback</li>
 *   <li>EMA(20) &gt; EMA(40) — day is in an uptrend (only buy pullbacks in uptrends)</li>
 *   <li>Bar count 4–26 — only trade 10:00–15:30 (avoid open/close volatility)</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: price reaches upper Bollinger Band (SMA20 + 2σ)</li>
 *   <li>RSI(5) &gt; 70 — momentum exhaustion</li>
 *   <li>Stop: 0.5% below entry</li>
 *   <li>Max 16 bars held (~4 hours)</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <p>This strategy requires volume data to work well, so it runs on individual
 * OMX30 stocks (which have volume), NOT on the ^OMX index.
 */
@Component
@Slf4j
public class VWAPMeanReversionIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {

    // ── Parameters ─────────────────────────────────────────────────────────
    private static final int    VWAP_PROXY_PERIOD = 20;  // SMA period as VWAP proxy
    private static final int    EMA_FAST          = 20;  // Trend filter fast
    private static final int    EMA_SLOW          = 40;  // Trend filter slow
    private static final int    RSI_PERIOD        = 5;   // Fast RSI
    private static final double RSI_ENTRY_LEVEL   = 35.0; // Oversold threshold
    private static final double RSI_EXIT_LEVEL    = 70.0; // Momentum exhaustion
    private static final int    BB_PERIOD         = 20;  // Bollinger Band period
    private static final double BB_K              = 2.0;  // Bollinger Band std dev multiplier
    private static final int    MIN_BAR_FOR_ENTRY = 4;   // ~10:00 (after opening chop)
    private static final int    MAX_BAR_FOR_ENTRY = 26;  // ~15:30 (before close)
    private static final double STOP_LOSS_PCT     = 0.5; // Hard stop below entry
    private static final int    MAX_BARS_HELD     = 16;  // ~4 hours

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator vwapProxy = new SMAIndicator(close, VWAP_PROXY_PERIOD);
        EMAIndicator emaFast = new EMAIndicator(close, EMA_FAST);
        EMAIndicator emaSlow = new EMAIndicator(close, EMA_SLOW);
        RSIIndicator rsi = new RSIIndicator(close, RSI_PERIOD);
        IntradayBarCountIndicator barCount = new IntradayBarCountIndicator(series);

        // Bollinger Bands for profit target
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(vwapProxy);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, BB_PERIOD);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, stdDev, DoubleNum.valueOf(BB_K));

        setIndicatorValues(close, "close");
        setIndicatorValues(vwapProxy, "sma20_vwap");
        setIndicatorValues(emaFast, "ema20");
        setIndicatorValues(emaSlow, "ema40");
        setIndicatorValues(rsi, "rsi5");

        // ── Entry ─────────────────────────────────────────────────────────
        // Price at or below VWAP proxy (mean-reversion pullback)
        Rule pullbackToMean = new UnderIndicatorRule(close, vwapProxy);
        // RSI oversold on pullback
        Rule rsiOversold = new UnderIndicatorRule(rsi, DoubleNum.valueOf(RSI_ENTRY_LEVEL));
        // Uptrend context: EMA(20) > EMA(40)
        Rule uptrend = new OverIndicatorRule(emaFast, emaSlow);
        // Time filter: bar 4–26 (10:00–15:30)
        Rule timeOk = new OverIndicatorRule(barCount, DoubleNum.valueOf(MIN_BAR_FOR_ENTRY - 1))
                .and(new UnderIndicatorRule(barCount, DoubleNum.valueOf(MAX_BAR_FOR_ENTRY + 1)));

        Rule entryRule = pullbackToMean.and(rsiOversold).and(uptrend).and(timeOk);

        // ── Exit ──────────────────────────────────────────────────────────
        // Profit target: price reaches upper Bollinger Band
        Rule profitTarget = new OverIndicatorRule(close, bbUpper);
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

    // ── Inner rule: time-based exit ────────────────────────────────────────

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
