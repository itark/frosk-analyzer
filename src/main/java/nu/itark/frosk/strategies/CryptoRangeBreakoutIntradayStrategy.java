package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.rules.AtrStopLossRule;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.CryptoRegimeRule;
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
 *   <li>{@link CryptoRegimeRule} — no long breakouts while BTC is below its
 *       daily SMA(20); altcoin breakouts fail in BTC downtrends</li>
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
        implements IIndicatorValue, CryptoIntradayStrategy {

    private static final int ATR_PERIOD = 14;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    /** Rolling range lookback, in 15m bars (24 = 6 hours). */
    @Value("${crypto.breakout.range.bars:24}")
    private int rangeBars;

    /** Minimum range width in percent for the breakout to be worth its fees. */
    @Value("${crypto.breakout.min.range.width.pct:1.5}")
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

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        // Previous N-bar high/low — shifted one bar so the current bar's own
        // high cannot be the level it "breaks out" of
        PreviousValueIndicator prevHigh = new PreviousValueIndicator(
                new HighestValueIndicator(new HighPriceIndicator(series), rangeBars));
        PreviousValueIndicator prevLow = new PreviousValueIndicator(
                new LowestValueIndicator(new LowPriceIndicator(series), rangeBars));
        EMAIndicator trendEma = new EMAIndicator(close, trendPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(prevHigh, "rangeHigh");
        setIndicatorValues(prevLow, "rangeLow");
        setIndicatorValues(trendEma, "trendEma");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule breakout = new CrossedUpIndicatorRule(close, prevHigh);
        Rule rangeWideEnough = new RangeWidthRule(prevHigh, prevLow, minRangeWidthPct);
        Rule trendUp = new OverIndicatorRule(close, trendEma);
        Rule regimeOk = new CryptoRegimeRule(series, cryptoRegimeService);

        Rule entryRule = breakout.and(rangeWideEnough).and(trendUp).and(regimeOk);

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
