package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoMarketRegime;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.service.LstmSignalFilterService;
import nu.itark.frosk.strategies.rules.AtrStopLossRule;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.CryptoMarketRegimeRule;
import nu.itark.frosk.strategies.rules.LstmSignalRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.ProfitLockTrailingRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * Crypto Rolling-Range Breakout — 15m Coinbase bars, 24/7.
 *
 * <p>Crypto has no opening range or overnight gap, so the equity ORB concept
 * is replaced by a rolling Donchian range: the high/low of the previous
 * {@code rangeBars} bars (6 hours on 15m bars by default).
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Close crosses above the previous {@code rangeBars}-bar high — a fresh
 *       breakout event, not a level (no re-entry chains after stop-outs)</li>
 *   <li>Range width at least {@code minRangeWidthPct} — the expected breakout
 *       move must clear the 1.2% taker round-trip with room to spare; tight
 *       ranges cannot pay for their own fees</li>
 *   <li>Close above EMA({@code trendPeriod}) — 24h trend agrees</li>
 *   <li>{@link CryptoMarketRegimeRule} requires {@code TRENDING_UP} — BTC above
 *       its SMA <em>and</em> ADX confirms an actual trend, not just price sitting
 *       above a lagging average. Breakouts are exactly the setup that fails hardest
 *       once a trend has gone sideways: confirmed 2026-07-22 to 2026-07-30, this
 *       strategy lost -232% at a 21.8% win rate while BTC stayed technically
 *       above its SMA the whole week but ADX had collapsed to RANGING</li>
 *   <li>{@link LstmSignalRule} — no-op unless {@code forecast.lstm.enabled=true}
 *       (crypto profile only for now, see {@link LstmSignalFilterService})</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>ATR trailing stop (chandelier), {@code atrTrailMult}×ATR(14)</li>
 *   <li>Initial ATR stop {@code atrStopMult}×ATR(14) below entry</li>
 *   <li>Max {@code maxBarsHeld} bars (~24h) — momentum that has not paid out
 *       in a day is not momentum</li>
 * </ul>
 *
 * <p>Backtested with the Coinbase taker fee (0.6%/trade) via
 * {@code BarSeriesService.resolveFee} — never the equity intraday fee.
 */
@Component
@Slf4j
public class CryptoRangeBreakoutIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Autowired
    private LstmSignalFilterService lstmSignalFilterService;

    /** Rolling range lookback, in 15m bars (24 = 6 hours). */
    @Value("${crypto.breakout.range.bars:24}")
    private int rangeBars;

    /** Minimum range width in percent for the breakout to be worth its fees. */
    @Value("${crypto.breakout.min.range.width.pct:2.0}")
    private double minRangeWidthPct;

    /** Trend filter EMA period in 15m bars (96 = 24 hours). */
    @Value("${crypto.breakout.trend.period:96}")
    private int trendPeriod;

    @Value("${crypto.breakout.atr.stop.mult:1.5}")
    private double atrStopMult;

    @Value("${crypto.breakout.atr.trail.mult:2.5}")
    private double atrTrailMult;

    /** Max bars held (96 = 24 hours on 15m bars). */
    @Value("${crypto.breakout.max.bars.held:96}")
    private int maxBarsHeld;

    private ClosePriceIndicator close;
    private PreviousValueIndicator prevHigh;
    private PreviousValueIndicator prevLow;
    private EMAIndicator trendEma;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        // Previous N-bar high/low — shifted one bar so the current bar's own
        // high cannot be the level it "breaks out" of
        prevHigh = new PreviousValueIndicator(
                new HighestValueIndicator(new HighPriceIndicator(series), rangeBars));
        prevLow = new PreviousValueIndicator(
                new LowestValueIndicator(new LowPriceIndicator(series), rangeBars));
        trendEma = new EMAIndicator(close, trendPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(prevHigh, "rangeHigh");
        setIndicatorValues(prevLow, "rangeLow");
        setIndicatorValues(trendEma, "trendEma");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule breakout = new CrossedUpIndicatorRule(close, prevHigh);
        Rule rangeWideEnough = new RangeWidthRule(prevHigh, prevLow, minRangeWidthPct);
        Rule trendUp = new OverIndicatorRule(close, trendEma);
        Rule regimeOk = new CryptoMarketRegimeRule(series, cryptoRegimeService, CryptoMarketRegime.TRENDING_UP);

        // LSTM signal filter — disabled by default everywhere except the crypto
        // profile; when disabled this is a permanent no-op and LstmSignalFilterService
        // never makes an HTTP call.
        Rule lstmOk = lstmSignalFilterService.isEnabled()
                ? new LstmSignalRule(series, lstmSignalFilterService)
                : new BooleanRule(true);

        Rule entryRule = breakout.and(rangeWideEnough).and(trendUp).and(regimeOk).and(lstmOk);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule trailingStop = new AtrTrailingStopRule(series, ATR_PERIOD, atrTrailMult);
        Rule initialStop = new AtrStopLossRule(series, ATR_PERIOD, atrStopMult);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);
        // Once unrealized profit ≥ 1.5%, activate tight 1.2×ATR chandelier to
        // lock in gains while the standard 1.8×ATR trail applies before that point.
        Rule profitLock = new ProfitLockTrailingRule(series, ATR_PERIOD, 1.5, 1.2);

        Rule exitRule = profitLock.or(trailingStop).or(initialStop).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /**
     * Neutralized — always BASE, so the position-size multiplier is a no-op.
     *
     * <p>Backtested 2026-08-10 on 506 closed trades (37 products, full 30-day
     * window, see {@code CryptoSignalStrengthBacktestIT}): the original scoring
     * (wider range + more decisive trend margin = more bonus conditions = bigger
     * size) was <b>inverted</b> — BASE won 32.6% at +0.448%/trade, ELEVATED and
     * STRONG both won only 28.5% at roughly -0.08%/trade. Same pattern as
     * {@link CryptoVWAPReversionIntradayStrategy}: the "wider/more extreme"
     * bonus criteria do not predict better outcomes here either. Do not
     * re-enable scoring here without re-backtesting against that same evidence.
     */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }

    /** Satisfied when (high − low) / low exceeds the configured percentage. */
    private static class RangeWidthRule extends AbstractRule {
        private final PreviousValueIndicator high;
        private final PreviousValueIndicator low;
        private final double minWidthPct;

        RangeWidthRule(PreviousValueIndicator high, PreviousValueIndicator low, double minWidthPct) {
            this.high = high;
            this.low = low;
            this.minWidthPct = minWidthPct;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            Num h = high.getValue(index);
            Num l = low.getValue(index);
            if (l.isZero() || l.isNaN() || h.isNaN()) {
                return false;
            }
            Num widthPct = h.minus(l).dividedBy(l).multipliedBy(l.numOf(100));
            return widthPct.doubleValue() >= minWidthPct;
        }
    }
}
