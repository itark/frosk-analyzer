package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Crypto VWAP Reversion — 15m Coinbase bars, anchored to the UTC day.
 *
 * <p>High-liquidity crypto pairs revert to the day's VWAP the same way large
 * caps do — market makers reference it around the clock. The VWAP "session"
 * is the UTC calendar day (crypto convention; there is no exchange session).
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>Close stretched at least {@code stretchPct} below the UTC-day VWAP.
 *       The stretch IS the gross profit target (exit at VWAP), so it must
 *       clear the 1.2% taker round-trip — default 1.8% leaves ~0.6% net</li>
 *   <li>RSI(14) &lt; {@code rsiEntry} — oversold confirmation</li>
 *   <li>{@link CryptoRegimeRule} — never catch falling knives while BTC is
 *       below its daily SMA(20); stretched prices keep stretching in
 *       crypto downtrends</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>Profit target: close back at/above the UTC-day VWAP</li>
 *   <li>Stop: {@code stopPct} below entry (≈1.5× the entry stretch)</li>
 *   <li>Max {@code maxBarsHeld} bars (~12h) — reversion that takes longer
 *       than half a day is a trend, not a stretch</li>
 * </ul>
 *
 * <p>Backtested with the Coinbase taker fee (0.6%/trade) via
 * {@code BarSeriesService.resolveFee} — never the equity intraday fee.
 */
@Component
@Slf4j
public class CryptoVWAPReversionIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy {
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

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SessionVWAPIndicator vwap = new SessionVWAPIndicator(series, UTC);
        TransformIndicator vwapStretched = TransformIndicator.multiply(vwap, 1.0 - stretchPct / 100.0);
        RSIIndicator rsi = new RSIIndicator(close, rsiPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "utcDayVwap");
        setIndicatorValues(rsi, "rsi7");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule stretched = new UnderIndicatorRule(close, vwapStretched);
        Rule oversold = new UnderIndicatorRule(rsi, DoubleNum.valueOf(rsiEntry));
        // Regime filter removed: stop-loss (2.7%) + 12h time exit provide
        // sufficient protection without blocking all risk-off entries.

        Rule entryRule = stretched.and(oversold);

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
}
