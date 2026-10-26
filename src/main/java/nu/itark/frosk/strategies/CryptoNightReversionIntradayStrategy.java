package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.indicators.RelativeVolumeIndicator;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import nu.itark.frosk.strategies.rules.CooldownAfterLossRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import nu.itark.frosk.strategies.rules.TimeWindowRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.ZoneId;
import java.util.List;

/**
 * <b>Brainstorm #2 — microstructure / night-pattern mean-reversion (Coinbase
 * spot, long).</b>
 *
 * <p>The research thesis (Admati &amp; Pfleiderer 1988, updated for crypto): in
 * the low-volume overnight hours (01–07 UTC, when both the EU and US desks are
 * away) an outsized price move on <em>thin</em> volume is far more often a
 * thin-book dislocation than a real trend, and it tends to revert once EU/US
 * volume returns. So this runs the project's existing VWAP mean-reversion idea
 * — but ONLY in that window, and ONLY when the bar is genuinely low-volume.
 *
 * <p>It is deliberately the {@link CryptoVWAPReversionIntradayStrategy} core
 * (stretch below the UTC-day VWAP + oversold RSI) with two extra entry gates
 * ({@link TimeWindowRule} + {@link RelativeVolumeIndicator}), not a new signal.
 * It does NOT force an exit when the window ends: the reversion target is the
 * VWAP, which is expected to be hit <em>after</em> 07 UTC as volume returns, so
 * {@code maxBarsHeld} is sized to hold into the EU/US session.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>Bar end time in [{@code startHourUtc}, {@code endHourUtc}) — the
 *       low-volume overnight window.</li>
 *   <li>Relative volume &lt; {@code relVolMax} — the move is on thin volume.</li>
 *   <li>Close stretched at least {@code stretchPct} below the UTC-day VWAP.</li>
 *   <li>RSI({@code rsiPeriod}) &lt; {@code rsiEntry} — oversold confirmation.</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>Close back at/above the UTC-day VWAP — reversion complete.</li>
 *   <li>{@code stopPct} below entry.</li>
 *   <li>Max {@code maxBarsHeld} bars.</li>
 * </ul>
 *
 * <p>Forward-data strategy — not yet validated on this project's data. Runs in
 * paper mode; places no real orders unless live trading is on. Gated by
 * {@code crypto.night.enabled}. Overfit guard: only a UTC-hour window and one
 * volume threshold beyond the inherited VWAP parameters.
 */
@Component
@Slf4j
public class CryptoNightReversionIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Value("${crypto.night.start.hour.utc:1}")
    private int startHourUtc;

    @Value("${crypto.night.end.hour.utc:7}")
    private int endHourUtc;

    /** Relative-volume lookback (672 = 7 days on 15m bars). */
    @Value("${crypto.night.relvol.lookback:672}")
    private int relVolLookback;

    /** Enter only when relative volume is below this — the thin-book condition. */
    @Value("${crypto.night.relvol.max:0.7}")
    private double relVolMax;

    /** Entry stretch below VWAP in percent — also the gross profit target. */
    @Value("${crypto.night.stretch.pct:1.2}")
    private double stretchPct;

    @Value("${crypto.night.rsi.period:7}")
    private int rsiPeriod;

    @Value("${crypto.night.rsi.entry:35.0}")
    private double rsiEntry;

    /** Hard stop below entry in percent. */
    @Value("${crypto.night.stop.pct:2.7}")
    private double stopPct;

    /** Max bars held (48 = 12h on 15m) — long enough to revert into the EU/US session. */
    @Value("${crypto.night.max.bars.held:48}")
    private int maxBarsHeld;

    /** Bars to wait after a losing exit before re-entering. 0 disables. */
    @Value("${crypto.night.cooldown.bars.after.loss:0}")
    private int cooldownBarsAfterLoss;

    private ClosePriceIndicator close;
    private SessionVWAPIndicator vwap;
    private RSIIndicator rsi;
    private RelativeVolumeIndicator relVol;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        vwap = new SessionVWAPIndicator(series, UTC);
        TransformIndicator vwapStretched = TransformIndicator.multiply(vwap, 1.0 - stretchPct / 100.0);
        rsi = new RSIIndicator(close, rsiPeriod);
        relVol = new RelativeVolumeIndicator(series, relVolLookback);

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "utcDayVwap");
        setIndicatorValues(rsi, "rsi" + rsiPeriod);
        setIndicatorValues(relVol, "relVol");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule inNightWindow = new TimeWindowRule(series, startHourUtc, endHourUtc);
        Rule lowVolume = new UnderIndicatorRule(relVol, DoubleNum.valueOf(relVolMax));
        Rule stretched = new UnderIndicatorRule(close, vwapStretched);
        Rule oversold = new UnderIndicatorRule(rsi, DoubleNum.valueOf(rsiEntry));
        Rule cooldown = new CooldownAfterLossRule(cooldownBarsAfterLoss);

        Rule entryRule = inNightWindow.and(lowVolume).and(stretched).and(oversold).and(cooldown);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule profitTarget = new OverIndicatorRule(close, vwap);
        Rule stopLoss = new StopLossRule(close, stopPct);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = profitTarget.or(stopLoss).or(timeExit);

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
