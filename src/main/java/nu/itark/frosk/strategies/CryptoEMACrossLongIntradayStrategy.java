package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.CryptoRegimeService;
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
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * EMA9/EMA21 cross — LONG side, 15m Coinbase bars.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>EMA(fast) crosses above EMA(slow)</li>
 *   <li>RSI({@code rsiPeriod}) &gt; 50 — momentum confirms the cross</li>
 *   <li>{@link CryptoRegimeRule} — BTC above its daily SMA(50)</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>EMA(fast) crosses back below EMA(slow)</li>
 *   <li>Max {@code maxBarsHeld} bars (~8h)</li>
 * </ul>
 *
 * <p>Short-side counterpart: {@link CryptoEMACrossShortIntradayStrategy}.
 */
@Component
@Slf4j
public class CryptoEMACrossLongIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy {
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

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) throw new IllegalArgumentException("BarSeries cannot be null");
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator emaF = new EMAIndicator(close, emaFast);
        EMAIndicator emaS = new EMAIndicator(close, emaSlow);
        RSIIndicator  rsi  = new RSIIndicator(close, rsiPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(emaF, "ema" + emaFast);
        setIndicatorValues(emaS, "ema" + emaSlow);
        setIndicatorValues(rsi,  "rsi" + rsiPeriod);

        // ── Entry ─────────────────────────────────────────────────────────
        Rule crossUp  = new CrossedUpIndicatorRule(emaF, emaS);
        Rule rsiAbove = new OverIndicatorRule(rsi, DoubleNum.valueOf(50));
        Rule regime   = new CryptoRegimeRule(series, cryptoRegimeService);

        Rule entryRule = crossUp.and(rsiAbove).and(regime);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule crossDown = new CrossedDownIndicatorRule(emaF, emaS);
        Rule timeExit  = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = crossDown.or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
