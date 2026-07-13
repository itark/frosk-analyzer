package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.rules.AtrStopLossShortRule;
import nu.itark.frosk.strategies.rules.AtrTrailingStopShortRule;
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
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.LocalTime;
import java.util.List;

/**
 * Crypto Rolling-Range Breakdown — short mirror of {@link CryptoRangeBreakoutIntradayStrategy}.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Close crosses below the previous {@code rangeBars}-bar low — fresh breakdown event</li>
 *   <li>Range width at least {@code minRangeWidthPct} — the expected move must clear fees</li>
 *   <li>Close below EMA({@code trendPeriod}) — 24h trend agrees (downtrend)</li>
 *   <li>Inverted {@link CryptoRegimeRule} — BTC below its daily SMA; only short in downtrend</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>ATR trailing stop (short chandelier) {@code atrTrailMult}×ATR(14) above lowest since entry</li>
 *   <li>Initial ATR stop {@code atrStopMult}×ATR(14) above entry</li>
 *   <li>Max {@code maxBarsHeld} bars (~24h)</li>
 * </ul>
 *
 * <p>Emits "SHRT" on entry and "COVR" on exit via the short-aware runner.
 */
@Component
@Slf4j
public class CryptoShortIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Value("${crypto.short.range.bars:24}")
    private int rangeBars;

    @Value("${crypto.short.min.range.width.pct:1.5}")
    private double minRangeWidthPct;

    @Value("${crypto.short.trend.period:96}")
    private int trendPeriod;

    @Value("${crypto.short.atr.stop.mult:1.5}")
    private double atrStopMult;

    @Value("${crypto.short.atr.trail.mult:2.5}")
    private double atrTrailMult;

    @Value("${crypto.short.max.bars.held:96}")
    private int maxBarsHeld;

    @Override
    public boolean isShort() { return true; }

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
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
        Rule breakdown   = new CrossedDownIndicatorRule(close, prevLow);
        Rule rangeWide   = new RangeWidthRule(prevHigh, prevLow, minRangeWidthPct);
        Rule trendDown   = new UnderIndicatorRule(close, trendEma);
        // Inverted regime: BTC below its SMA — risk-off is required for shorts
        Rule regimeOk    = new CryptoRegimeRule(series, cryptoRegimeService, true);
        // Block during 17:30–19:00 UTC: US session evening volatility spikes crush shorts
        Rule notUsEvening = new TimeGatingRule(LocalTime.of(17, 30), LocalTime.of(19, 0));

        Rule entryRule = breakdown.and(rangeWide).and(trendDown).and(regimeOk).and(notUsEvening);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule trailingStop = new AtrTrailingStopShortRule(series, ATR_PERIOD, atrTrailMult);
        Rule initialStop  = new AtrStopLossShortRule(series, ATR_PERIOD, atrStopMult);
        Rule timeExit     = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = trailingStop.or(initialStop).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

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
            if (l.isZero() || l.isNaN() || h.isNaN()) return false;
            Num widthPct = h.minus(l).dividedBy(l).multipliedBy(l.numOf(100));
            return widthPct.doubleValue() >= minWidthPct;
        }
    }
}
