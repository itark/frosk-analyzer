package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * Daily BTC-EUR trend follower — EMA({@value #DEFAULT_EMA_FAST})/EMA({@value
 * #DEFAULT_EMA_SLOW}) crossover with an ADX({@value #DEFAULT_ADX_PERIOD})
 * &gt; {@code adxThreshold} trend-strength filter.
 *
 * <p>This is the ta4j expression of the signal — long entry on a golden cross
 * while ADX confirms a trend, exit on a death cross. The three-state
 * LONG/SHORT/NEUTRAL signal a human acts on (BULL/BEAR X2 on Avanza, or hold
 * cash) is derived from the same EMA/ADX values by
 * {@link nu.itark.frosk.service.CryptoBtcTrendSignalService}, which is what
 * the dashboard card reads. ta4j strategies are long-only, so the SHORT leg
 * has no ta4j entry rule here — it lives only in the signal service.
 *
 * <p>Registered in {@code StrategiesMap} for backtest metrics on BTC-EUR's
 * daily history, but kept out of the generic {@code runCryptoStrategies()}
 * batch (which runs every strategy against every COINBASE security) via
 * {@code frosk.strategies.exclude} — it is BTC-only by design and is driven
 * by its own scheduled service.
 */
@Component
public class CryptoBTCTrendStrategy extends AbstractStrategy implements IIndicatorValue {

    public static final int DEFAULT_EMA_FAST = 10;
    public static final int DEFAULT_EMA_SLOW = 20;
    public static final int DEFAULT_ADX_PERIOD = 14;
    public static final double DEFAULT_ADX_THRESHOLD = 20.0;

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    @Value("${crypto.btctrend.ema.fast:10}")
    private int emaFast;

    @Value("${crypto.btctrend.ema.slow:20}")
    private int emaSlow;

    @Value("${crypto.btctrend.adx.period:14}")
    private int adxPeriod;

    @Value("${crypto.btctrend.adx.threshold:20.0}")
    private double adxThreshold;

    private EMAIndicator emaFastIndicator;
    private EMAIndicator emaSlowIndicator;
    private ADXIndicator adxIndicator;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        emaFastIndicator = new EMAIndicator(close, emaFast);
        emaSlowIndicator = new EMAIndicator(close, emaSlow);
        adxIndicator = new ADXIndicator(series, adxPeriod);

        setIndicatorValues(emaFastIndicator, "ema" + emaFast);
        setIndicatorValues(emaSlowIndicator, "ema" + emaSlow);
        setIndicatorValues(adxIndicator, "adx" + adxPeriod);

        Rule goldenCross = new CrossedUpIndicatorRule(emaFastIndicator, emaSlowIndicator);
        Rule trendStrong = new OverIndicatorRule(adxIndicator, adxThreshold);
        Rule entryRule = goldenCross.and(trendStrong);

        Rule exitRule = new CrossedDownIndicatorRule(emaFastIndicator, emaSlowIndicator);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    public int getEmaFast() {
        return emaFast;
    }

    public int getEmaSlow() {
        return emaSlow;
    }

    public int getAdxPeriod() {
        return adxPeriod;
    }

    public double getAdxThreshold() {
        return adxThreshold;
    }
}
