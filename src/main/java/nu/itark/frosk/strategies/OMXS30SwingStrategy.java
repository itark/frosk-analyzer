package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.StopGainRule;

import java.util.List;

/**
 * OMXS30 Swing Trading Strategy — "Nordic Momentum Filter"
 *
 * ENTRY  : EMA(20) > EMA(50)                          — uptrend confirmed
 *           AND RSI(14) crosses above 40               — momentum turning up
 *           AND OBV > OBV SMA(5)                       — volume confirms direction
 *
 * EXIT   : RSI(14) crosses above 65                   — take profit (overbought)
 *           OR price drops 2×ATR(14) below entry       — ATR stop-loss
 *           OR price rises 3×ATR(14) above entry       — ATR take-profit
 *           OR 10 bars elapsed since entry             — time-based exit
 *
 * Suitable for daily bars on OMXS30 stocks or the XACT OMXS30 ETF.
 */
@Component
@Slf4j
public class OMXS30SwingStrategy extends AbstractStrategy implements IIndicatorValue {

    private static final int    EMA_FAST        = 20;
    private static final int    EMA_SLOW        = 50;
    private static final int    RSI_PERIOD      = 14;
    private static final int    ATR_PERIOD      = 14;
    private static final int    OBV_SMOOTH      = 5;
    private static final double RSI_ENTRY_CROSS = 40.0;
    private static final double RSI_EXIT_LEVEL  = 65.0;
    private static final double ATR_STOP_MULT   = 2.0;
    private static final double ATR_TARGET_MULT = 3.0;
    private static final int    MAX_BARS_HELD   = 10;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        EMAIndicator emaFast = new EMAIndicator(closePrice, EMA_FAST);
        EMAIndicator emaSlow = new EMAIndicator(closePrice, EMA_SLOW);
        RSIIndicator rsi     = new RSIIndicator(closePrice, RSI_PERIOD);

        OnBalanceVolumeIndicator obv       = new OnBalanceVolumeIndicator(series);
        SMAIndicator             obvSmooth = new SMAIndicator(obv, OBV_SMOOTH);

        setIndicatorValues(emaFast, "ema20");
        setIndicatorValues(emaSlow, "ema50");
        setIndicatorValues(rsi, "rsi14");

        // ── Entry rules ──────────────────────────────────────────────────────
        Rule trendUp    = new OverIndicatorRule(emaFast, emaSlow);
        Rule rsiEntry   = new CrossedUpIndicatorRule(rsi, RSI_ENTRY_CROSS);
        Rule obvBullish = new OverIndicatorRule(obv, obvSmooth);
        Rule entryRule  = trendUp.and(rsiEntry).and(obvBullish);

        // ── Exit rules ───────────────────────────────────────────────────────
        Rule takeProfitRsi = new CrossedUpIndicatorRule(rsi, RSI_EXIT_LEVEL);
        Rule stopLossAtr   = new AtrStopLossRule(series, ATR_PERIOD, ATR_STOP_MULT);
        Rule takeProfitAtr = new StopGainRule(closePrice, ATR_TARGET_MULT * 2);
        Rule timeExit      = new MaxBarsHeldRule(MAX_BARS_HELD);
        Rule exitRule      = takeProfitRsi.or(stopLossAtr).or(takeProfitAtr).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    // ── ATR-based stop-loss: exits when price drops multiplier×ATR below entry ─
    private static class AtrStopLossRule extends AbstractRule {

        private final BarSeries           series;
        private final ATRIndicator        atr;
        private final double              multiplier;
        private final ClosePriceIndicator close;

        AtrStopLossRule(BarSeries series, int atrPeriod, double multiplier) {
            this.series     = series;
            this.atr        = new ATRIndicator(series, atrPeriod);
            this.multiplier = multiplier;
            this.close      = new ClosePriceIndicator(series);
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
                return false;
            }
            int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
            Num entryPrice = close.getValue(entryIndex);
            Num stopLevel  = entryPrice.minus(atr.getValue(entryIndex).multipliedBy(series.numOf(multiplier)));
            return close.getValue(index).isLessThanOrEqual(stopLevel);
        }
    }

    // ── Time-based exit: close position after maxBars bars ───────────────────
    private static class MaxBarsHeldRule extends AbstractRule {

        private final int maxBars;

        MaxBarsHeldRule(int maxBars) {
            this.maxBars = maxBars;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (tradingRecord == null || tradingRecord.getCurrentPosition().isNew()) {
                return false;
            }
            int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
            return (index - entryIndex) >= maxBars;
        }
    }
}
