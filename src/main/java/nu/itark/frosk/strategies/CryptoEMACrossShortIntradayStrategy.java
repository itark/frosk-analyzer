package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.rules.CryptoRegimeRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.TimeGatingRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.LocalTime;
import java.util.List;

/**
 * EMA9/EMA21 cross — SHORT side, 15m Coinbase bars.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>EMA(fast) crosses below EMA(slow)</li>
 *   <li>RSI({@code rsiPeriod}) &lt; 50 — momentum confirms the cross</li>
 *   <li>Inverted {@link CryptoRegimeRule} — BTC below its daily SMA(50); only
 *       short while BTC is in a downtrend</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>EMA(fast) crosses back above EMA(slow)</li>
 *   <li>Max {@code maxBarsHeld} bars (~8h)</li>
 * </ul>
 *
 * <p>Emits "SHRT"/"COVR" via the runner's short-aware signal path.
 * Long-side counterpart: {@link CryptoEMACrossLongIntradayStrategy}.
 */
@Component
@Slf4j
public class CryptoEMACrossShortIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Value("${crypto.emacross.ema.fast:9}")
    private int emaFast;

    @Value("${crypto.emacross.ema.slow:21}")
    private int emaSlow;

    @Value("${crypto.emacross.rsi.period:7}")
    private int rsiPeriod;

    @Value("${crypto.emacross.max.bars.held:32}")
    private int maxBarsHeld;

    private ClosePriceIndicator close;
    private EMAIndicator emaF;
    private EMAIndicator emaS;
    private RSIIndicator rsi;

    @Override
    public boolean isShort() { return true; }

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) throw new IllegalArgumentException("BarSeries cannot be null");
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        emaF = new EMAIndicator(close, emaFast);
        emaS = new EMAIndicator(close, emaSlow);
        rsi  = new RSIIndicator(close, rsiPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(emaF, "ema" + emaFast);
        setIndicatorValues(emaS, "ema" + emaSlow);
        setIndicatorValues(rsi,  "rsi" + rsiPeriod);

        // ── Entry ─────────────────────────────────────────────────────────
        Rule crossDown = new CrossedDownIndicatorRule(emaF, emaS);
        Rule rsiBelow  = new UnderIndicatorRule(rsi, DoubleNum.valueOf(50));
        // Inverted regime: BTC below SMA(50) — required for short entries
        Rule regime    = new CryptoRegimeRule(series, cryptoRegimeService, true);
        // Block during 06:00–10:00 UTC: EU equity open causes crypto rallies that crush shorts
        Rule notEuOpenRally = new TimeGatingRule(LocalTime.of(6, 0), LocalTime.of(10, 0));

        Rule entryRule = crossDown.and(rsiBelow).and(regime).and(notEuOpenRally);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule crossUp  = new CrossedUpIndicatorRule(emaF, emaS);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = crossUp.or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Neutralized — always BASE, so the position-size multiplier is a no-op.
     *
     * <p>Unlike {@link CryptoVWAPReversionIntradayStrategy} and
     * {@link CryptoRangeBreakoutIntradayStrategy} (both backtested 2026-08-10 and
     * found <b>inverted</b>), this one has not been backtested at all: the
     * inverted regime gate ({@link CryptoRegimeRule} requiring BTC below its
     * SMA(50)) means it only fires during a sustained downtrend, and BTC was
     * in an uptrend for the entirety of the available 30-day window
     * ({@code CryptoSignalStrengthBacktestIT} — 0 closed trades). Was a mirror
     * of {@link CryptoEMACrossLongIntradayStrategy}'s (validated, correctly
     * ordered) scoring, on the unverified assumption the long-side logic
     * transfers to the short side. Re-enable and re-backtest once BTC has
     * actually traded below its SMA(50) for long enough to produce a sample —
     * do not re-enable on the strength of the long-side result alone.
     */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }
}
