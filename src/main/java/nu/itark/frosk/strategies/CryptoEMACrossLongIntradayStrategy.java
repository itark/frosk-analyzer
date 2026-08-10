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
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    /** RSI past this (vs. the 50 entry threshold) counts as a bonus condition. */
    private static final double RSI_STRONG_THRESHOLD = 60.0;
    /** EMA spread past this fraction of price counts as a decisive, not marginal, cross. */
    private static final double EMA_SPREAD_STRONG_PCT = 0.3;

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

    /**
     * Bonus conditions: RSI decisively above 50 (not just past it), and an EMA
     * spread wide enough to be a decisive cross rather than a marginal one.
     */
    @Override
    public SignalStrength getSignalStrength(int index) {
        int bonuses = 0;
        if (rsi.getValue(index).doubleValue() > RSI_STRONG_THRESHOLD) {
            bonuses++;
        }
        double closeVal = close.getValue(index).doubleValue();
        if (closeVal > 0) {
            double spreadPct = 100.0 * (emaF.getValue(index).doubleValue() - emaS.getValue(index).doubleValue()) / closeVal;
            if (spreadPct >= EMA_SPREAD_STRONG_PCT) {
                bonuses++;
            }
        }
        return bonuses >= 2 ? SignalStrength.STRONG : bonuses == 1 ? SignalStrength.ELEVATED : SignalStrength.BASE;
    }
}
