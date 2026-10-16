package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.LstmSignalFilterService;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import nu.itark.frosk.strategies.rules.CooldownAfterLossRule;
import nu.itark.frosk.strategies.rules.LstmSignalRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import nu.itark.frosk.strategies.rules.TimeWindowRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.ZoneId;
import java.util.List;

/**
 * Crypto VWAP Reversion — 15m Coinbase bars, anchored to the UTC day.
 *
 * <p>High-liquidity crypto pairs revert to the day's VWAP the same way large
 * caps do — market makers reference it around the clock. The VWAP "session"
 * is the UTC calendar day (crypto convention; there is no exchange session).
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Close stretched at least {@code stretchPct} below the UTC-day VWAP.
 *       The stretch IS the gross profit target (exit at VWAP), so it must clear
 *       the taker round trip with room to spare. At the current 0.10% taker tier
 *       (0.20% round trip) the configured 1.2% stretch leaves ~1.0% net. This
 *       margin is tier-dependent: at the 0.60% entry-tier rate the round trip is
 *       1.2%, i.e. exactly the stretch, and a perfect winner nets zero — re-check
 *       this setting whenever the fee tier changes</li>
 *   <li>RSI(14) &lt; {@code rsiEntry} — oversold confirmation</li>
 * </ul>
 *
 * <p>Regime history: the older {@code CryptoRegimeRule} (BTC risk-on/off) was
 * removed as a directional macro gate, and the GARCH+ADX SIDEWAYS-only gate
 * ({@code GarchRegimeRule}) was removed 2026-09-17 so this strategy traded in
 * every market structure, relying on the stop-loss and time exit alone.
 *
 * <p>Per-ticker trend filter (re-introduced 2026-10-07, {@code
 * crypto.vwap.trend.filter.enabled}, default OFF): entry only while the ticker's
 * own SMA({@code trendSmaBars}) is above its value {@code trendSlopeBars} ago,
 * i.e. the 24h average is not falling. Without a gate the strategy bought dips in
 * steady downtrends where VWAP follows price down and no reversion comes
 * (2026-10-06: 33 of 37 products down, traded alts -3..-9%). Measured on 453
 * trades 2026-09-08..10-06: entries with a falling 24h SMA lost -45.71 EUR (175
 * trades), the rest made +39.74 EUR (278). It is the ticker's OWN trend on
 * purpose — a BTC-trend proxy pointed the wrong way in the same data (VWAP lost
 * -54 EUR while BTC's SMA was rising). In-sample over 27 days and not monotone
 * across slope buckets: validate with {@code StrategyComparisonReportIT} before
 * trusting the number.
 *
 * <p>UTC entry window (2026-10-16, {@code crypto.vwap.time.window.enabled}, default
 * OFF): entry only while the signal bar's end time is in [{@code start.hour.utc},
 * {@code end.hour.utc}), via {@link TimeWindowRule}. Motivated by external BTC
 * seasonality research (21:00–23:00 UTC). In-house evidence is weak: H1 in
 * {@code ~/itark/PREREG_vwap_signal_filters.md} (VWAP entries at 21–22 UTC, 400 days)
 * was not recommended (+0.07 pp, CI spanning zero, halves of opposite sign), and
 * with the window on, the paper track record no longer contains out-of-window
 * entries — which {@code ~/itark/PREREG_vwap_seasonality_window.md} needs for its
 * comparison.
 *
 * <p>Optional LSTM confirmation (2026-10-05, OFF by default): {@link
 * LstmSignalRule} gates entry only when BOTH {@code forecast.lstm.enabled=true}
 * (crypto-wide microservice switch, currently false — its former consumers
 * EMACrossLong and RangeBreakout were removed 2026-10-05) AND {@code
 * crypto.vwap.lstm.filter.enabled=true} (this strategy's own switch, default
 * false) are set. This is this project's only proven live edge, so the filter
 * must not go live here on a single shared-flag flip. Measure with {@code
 * StrategyComparisonReportIT#cryptoIntradayReport} (filter on vs off) before
 * enabling in application-crypto.properties.
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: close back at/above the UTC-day VWAP</li>
 *   <li>Stop: {@code stopPct} below entry (≈1.5× the entry stretch)</li>
 *   <li>Max {@code maxBarsHeld} bars (~12h) — reversion that takes longer
 *       than half a day is a trend, not a stretch</li>
 * </ul>
 *
 * <p>Backtested with the Coinbase taker fee via {@code TransactionFeeService}
 * — never the equity intraday fee.
 */
@Component
@Slf4j
public class CryptoVWAPReversionIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Value("${crypto.vwap.rsi.period:7}")
    private int rsiPeriod;

    /** Entry stretch below VWAP in percent — also the gross profit target. */
    @Value("${crypto.vwap.stretch.pct:1.8}")
    private double stretchPct;

    @Value("${crypto.vwap.rsi.entry:35.0}")
    private double rsiEntry;

    /** Hard stop below entry in percent (≈1.5× the stretch). */
    @Value("${crypto.vwap.stop.pct:2.7}")
    private double stopPct;

    /** Max bars held (48 = 12 hours on 15m bars). */
    @Value("${crypto.vwap.max.bars.held:48}")
    private int maxBarsHeld;

    /** Bars to wait after a losing exit before re-entering — see {@link CooldownAfterLossRule}. 0 disables. */
    @Value("${crypto.vwap.cooldown.bars.after.loss:0}")
    private int cooldownBarsAfterLoss;

    /** Per-ticker trend filter — see class javadoc. Default OFF. */
    @Value("${crypto.vwap.trend.filter.enabled:false}")
    private boolean trendFilterEnabled;

    /** SMA length for the trend filter (96 = 24h on 15m bars). */
    @Value("${crypto.vwap.trend.sma.bars:96}")
    private int trendSmaBars;

    /** Lookback for the SMA slope (24 = 6h on 15m bars). */
    @Value("${crypto.vwap.trend.slope.bars:24}")
    private int trendSlopeBars;

    /** UTC entry-window gate — see class javadoc. Default OFF. */
    @Value("${crypto.vwap.time.window.enabled:false}")
    private boolean timeWindowEnabled;

    /** First UTC hour (inclusive) of the entry window, judged on the signal bar's end time. */
    @Value("${crypto.vwap.time.window.start.hour.utc:21}")
    private int timeWindowStartHourUtc;

    /** UTC hour (exclusive) that closes the entry window. */
    @Value("${crypto.vwap.time.window.end.hour.utc:23}")
    private int timeWindowEndHourUtc;

    /** This strategy's own switch for the optional LSTM gate — see class javadoc. Default OFF. */
    @Value("${crypto.vwap.lstm.filter.enabled:false}")
    private boolean lstmFilterEnabled;

    @Autowired
    private LstmSignalFilterService lstmSignalFilterService;

    private ClosePriceIndicator close;
    private SessionVWAPIndicator vwap;
    private RSIIndicator rsi;

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

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "utcDayVwap");
        setIndicatorValues(rsi, "rsi7");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule stretched = new UnderIndicatorRule(close, vwapStretched);
        Rule oversold = new UnderIndicatorRule(rsi, DoubleNum.valueOf(rsiEntry));
        // CryptoRegimeRule (BTC risk-on/off) and the GARCH+ADX SIDEWAYS-only gate
        // (removed 2026-09-17) are gone; the per-ticker SMA-slope filter below
        // (2026-10-07) replaces them — see class javadoc.
        Rule trendOk;
        if (trendFilterEnabled) {
            SMAIndicator trendSma = new SMAIndicator(close, trendSmaBars);
            trendOk = new OverIndicatorRule(trendSma, new PreviousValueIndicator(trendSma, trendSlopeBars));
        } else {
            trendOk = BooleanRule.TRUE;
        }

        // Optional LSTM confirmation gate — see class javadoc. No-op (BooleanRule.TRUE)
        // unless this strategy's OWN flag is set, even though the shared
        // forecast.lstm.enabled flag is already true for the crypto profile.
        Rule lstmGate = (lstmFilterEnabled && lstmSignalFilterService.isEnabled())
                ? new LstmSignalRule(series, lstmSignalFilterService)
                : BooleanRule.TRUE;

        Rule cooldown = new CooldownAfterLossRule(cooldownBarsAfterLoss);

        Rule inTimeWindow = timeWindowEnabled
                ? new TimeWindowRule(series, timeWindowStartHourUtc, timeWindowEndHourUtc)
                : BooleanRule.TRUE;

        Rule entryRule = stretched.and(oversold).and(trendOk).and(lstmGate).and(cooldown).and(inTimeWindow);

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

    /**
     * Neutralized — always BASE, so the position-size multiplier is a no-op.
     *
     * <p>Backtested 2026-08-10 on 561 closed trades (37 products, full 30-day
     * window, see {@code CryptoSignalStrengthBacktestIT}): the original scoring
     * (deeper VWAP stretch + more oversold RSI = more bonus conditions = bigger
     * size) was <b>inverted</b> — BASE won 60.0% at +0.231%/trade, STRONG won
     * only 47.3% at -0.325%/trade, monotonically worse at every tier. For a
     * mean-reversion strategy a more extreme dislocation is more often a real
     * breakdown than an extra-attractive dip — exactly what this class's own
     * entry-rule comment already warned about ("never catch falling knives...
     * stretched prices keep stretching in crypto downtrends"). Do not re-enable
     * scoring here without re-backtesting against that same evidence.
     */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }
}
