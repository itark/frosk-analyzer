package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.indicators.VolatilityRegimeIndicator;
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
 * <b>Strategi 5 — Volatility-regime switching wrapped around the EMA/ADX
 * momentum core.</b>
 *
 * <p>A meta-layer, not a new signal: it takes the same EMA/ADX golden-cross
 * entry as {@link CryptoEmaAdxTrendIntradayStrategy} and makes the volatility
 * regime an explicit, hysteresis-stabilised switch (see {@link
 * VolatilityRegimeIndicator} — 20/80 ATR-percentile thresholds, 10-point
 * hysteresis, 2-bar confirmation, 100-bar lookback, per the LuxAlgo defaults in
 * the research). The thesis (Banerjee SSRN 2025; Dobrynskaya 2023) is that
 * momentum pays in calm volatility and that high-volatility regimes should be
 * sat out rather than traded — the layer's value is drawdown control, not new
 * alpha, which is why it is built as a wrapper once the core is understood.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>EMA({@code emaFast}) crosses up through EMA({@code emaSlow}).</li>
 *   <li>ADX({@code adxPeriod}) &gt; {@code adxThreshold}.</li>
 *   <li>Volatility regime is NOT {@code STRESSED} (QUIET or NORMAL).</li>
 *   <li>Optional {@link CryptoRegimeRule} risk-on macro gate.</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>Volatility regime turns {@code STRESSED} — flatten, per the recipe.</li>
 *   <li>EMA({@code emaFast}) crosses back down through EMA({@code emaSlow}).</li>
 *   <li>{@link AtrStopLossRule}: {@code atrStopMult}× ATR below entry.</li>
 *   <li>Max {@code maxBarsHeld} bars.</li>
 * </ul>
 *
 * <p><b>Regime sizing.</b> The research sizes full in QUIET and half in NORMAL.
 * The project's {@link SignalStrength} enum has no sub-1.0 tier (BASE=1.0,
 * ELEVATED, STRONG), so the full/half distinction is expressed instead as
 * ELEVATED in QUIET and BASE in NORMAL — the same relative ordering (calm gets
 * more size) within the available tiers. This is the one crypto strategy that
 * deliberately uses non-BASE sizing, because regime-conditional sizing is its
 * whole point; it only ever sizes <em>up</em> in the calmer regime, and in paper
 * mode until validated.
 *
 * <p>Forward-data strategy — gated by {@code crypto.volregime.enabled}; places
 * no real orders unless live trading is on.
 */
@Component
@Slf4j
public class CryptoVolRegimeMomentumIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Value("${crypto.volregime.ema.fast:9}")
    private int emaFast;

    @Value("${crypto.volregime.ema.slow:21}")
    private int emaSlow;

    @Value("${crypto.volregime.adx.period:14}")
    private int adxPeriod;

    @Value("${crypto.volregime.adx.threshold:20.0}")
    private double adxThreshold;

    @Value("${crypto.volregime.atr.lookback:100}")
    private int atrLookback;

    @Value("${crypto.volregime.quiet.pct:20.0}")
    private double quietPct;

    @Value("${crypto.volregime.stressed.pct:80.0}")
    private double stressedPct;

    @Value("${crypto.volregime.hysteresis:10.0}")
    private double hysteresis;

    @Value("${crypto.volregime.confirm.bars:2}")
    private int confirmBars;

    @Value("${crypto.volregime.atr.stop.mult:1.5}")
    private double atrStopMult;

    @Value("${crypto.volregime.max.bars.held:32}")
    private int maxBarsHeld;

    @Value("${crypto.volregime.macro.filter.enabled:true}")
    private boolean macroFilterEnabled;

    private ClosePriceIndicator close;
    private EMAIndicator emaF;
    private EMAIndicator emaS;
    private ADXIndicator adx;
    private VolatilityRegimeIndicator regime;

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
        regime = new VolatilityRegimeIndicator(series, ATR_PERIOD, atrLookback,
                quietPct, stressedPct, hysteresis, confirmBars);

        setIndicatorValues(close, "close");
        setIndicatorValues(emaF, "ema" + emaFast);
        setIndicatorValues(emaS, "ema" + emaSlow);
        setIndicatorValues(adx, "adx" + adxPeriod);
        setIndicatorValues(regime, "volRegime");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule crossUp = new CrossedUpIndicatorRule(emaF, emaS);
        Rule trendStrong = new OverIndicatorRule(adx, DoubleNum.valueOf(adxThreshold));
        // regime code < STRESSED(2) → QUIET or NORMAL
        Rule notStressed = new UnderIndicatorRule(regime, DoubleNum.valueOf(VolatilityRegimeIndicator.STRESSED));
        Rule macroOk = macroFilterEnabled
                ? new CryptoRegimeRule(series, cryptoRegimeService)
                : BooleanRule.TRUE;

        Rule entryRule = crossUp.and(trendStrong).and(notStressed).and(macroOk);

        // ── Exit ──────────────────────────────────────────────────────────
        // regime code >= STRESSED(2): use > (STRESSED - 0.5) to catch the integer code exactly.
        Rule stressedExit = new OverIndicatorRule(regime, DoubleNum.valueOf(VolatilityRegimeIndicator.STRESSED - 0.5));
        Rule crossDown = new CrossedDownIndicatorRule(emaF, emaS);
        Rule atrStop = new AtrStopLossRule(series, ATR_PERIOD, atrStopMult);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = stressedExit.or(crossDown).or(atrStop).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Regime-conditional sizing: ELEVATED in the QUIET regime, BASE otherwise —
     * see the "Regime sizing" note in the class javadoc. Reads the regime
     * indicator built in {@link #buildStrategy}; falls back to BASE if called
     * before the strategy is built.
     */
    @Override
    public SignalStrength getSignalStrength(int index) {
        if (regime == null) {
            return SignalStrength.BASE;
        }
        return regime.getValue(index).intValue() == VolatilityRegimeIndicator.QUIET
                ? SignalStrength.ELEVATED
                : SignalStrength.BASE;
    }
}
