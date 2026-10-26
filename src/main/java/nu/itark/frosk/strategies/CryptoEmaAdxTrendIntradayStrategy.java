package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.indicators.AtrPercentileIndicator;
import nu.itark.frosk.strategies.rules.AtrStopLossRule;
import nu.itark.frosk.strategies.rules.CryptoRegimeRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * <b>Strategi 1 — EMA cross with an ADX trend filter and volatility-regime gate
 * (LONG side).</b>
 *
 * <p>The core momentum recipe from the research: an EMA({@code emaFast})/EMA({@code
 * emaSlow}) golden cross for direction, an ADX({@code adxPeriod}) &gt;
 * {@code adxThreshold} filter so the cross only counts while a measurable trend
 * exists (the single most-cited chop filter for crypto systems), and a
 * volatility-regime gate that stands aside when ATR is in its top percentile —
 * the "stressed" state where fresh breakouts tend to be whipsaws. A practitioner
 * study on 302k+ 15m candles (BTC/ETH/SOL, 2023–2026) found an SMA-trend
 * baseline consistently improved momentum results; Shen, Urquhart &amp; Wang
 * (2022) document that BTC intraday momentum pays "especially in periods of a
 * market downturn".
 *
 * <p>This is the long-only ta4j expression (Coinbase cannot short; the short leg
 * lives in {@link CryptoEMACrossShortIntradayStrategy}). The since-removed
 * CryptoEMACrossLongIntradayStrategy was a bare EMA cross with no ADX or
 * volatility gate — this strategy is that idea done with the research's filters.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>EMA({@code emaFast}) crosses up through EMA({@code emaSlow}).</li>
 *   <li>ADX({@code adxPeriod}) &gt; {@code adxThreshold} — a real trend, not a dip.</li>
 *   <li>{@link AtrPercentileIndicator} &lt; {@code stressedPct} — not in the
 *       stressed volatility regime.</li>
 *   <li>Optional {@link CryptoRegimeRule} risk-on macro gate
 *       ({@code crypto.emaadx.macro.filter.enabled}, default on).</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>EMA({@code emaFast}) crosses back down through EMA({@code emaSlow}).</li>
 *   <li>{@link AtrStopLossRule}: {@code atrStopMult}× ATR below entry.</li>
 *   <li>Max {@code maxBarsHeld} bars (~8h on 15m).</li>
 * </ul>
 *
 * <p>Forward-data strategy — not yet validated on this project's data. Runs in
 * paper mode to accumulate history for a later pre-registered test; places no
 * real orders unless live trading is on. Gated by {@code crypto.emaadx.enabled}.
 */
@Component
@Slf4j
public class CryptoEmaAdxTrendIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Value("${crypto.emaadx.ema.fast:9}")
    private int emaFast;

    @Value("${crypto.emaadx.ema.slow:21}")
    private int emaSlow;

    @Value("${crypto.emaadx.adx.period:14}")
    private int adxPeriod;

    @Value("${crypto.emaadx.adx.threshold:20.0}")
    private double adxThreshold;

    /** ATR-percentile lookback (100 bars, per the research default). */
    @Value("${crypto.emaadx.atr.lookback:100}")
    private int atrLookback;

    /** Block entries at/above this ATR percentile — the "stressed" regime. */
    @Value("${crypto.emaadx.stressed.pct:80.0}")
    private double stressedPct;

    @Value("${crypto.emaadx.atr.stop.mult:1.5}")
    private double atrStopMult;

    @Value("${crypto.emaadx.max.bars.held:32}")
    private int maxBarsHeld;

    @Value("${crypto.emaadx.macro.filter.enabled:true}")
    private boolean macroFilterEnabled;

    private ClosePriceIndicator close;
    private EMAIndicator emaF;
    private EMAIndicator emaS;
    private ADXIndicator adx;
    private AtrPercentileIndicator atrPct;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        emaF = new EMAIndicator(close, emaFast);
        emaS = new EMAIndicator(close, emaSlow);
        adx = new ADXIndicator(series, adxPeriod);
        atrPct = new AtrPercentileIndicator(series, ATR_PERIOD, atrLookback);

        setIndicatorValues(close, "close");
        setIndicatorValues(emaF, "ema" + emaFast);
        setIndicatorValues(emaS, "ema" + emaSlow);
        setIndicatorValues(adx, "adx" + adxPeriod);
        setIndicatorValues(atrPct, "atrPct");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule crossUp = new CrossedUpIndicatorRule(emaF, emaS);
        Rule trendStrong = new OverIndicatorRule(adx, DoubleNum.valueOf(adxThreshold));
        Rule notStressed = new UnderIndicatorRule(atrPct, DoubleNum.valueOf(stressedPct));
        Rule macroOk = macroFilterEnabled
                ? new CryptoRegimeRule(series, cryptoRegimeService)
                : BooleanRule.TRUE;

        Rule entryRule = crossUp.and(trendStrong).and(notStressed).and(macroOk);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule crossDown = new CrossedDownIndicatorRule(emaF, emaS);
        Rule atrStop = new AtrStopLossRule(series, ATR_PERIOD, atrStopMult);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = crossDown.or(atrStop).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /** Neutralized — no un-backtested sizing, consistent with the other crypto strategies. */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }
}
