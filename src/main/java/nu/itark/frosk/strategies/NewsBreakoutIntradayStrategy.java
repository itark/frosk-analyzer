package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.GapPercentIndicator;
import nu.itark.frosk.strategies.indicators.PreviousDayCloseIndicator;
import nu.itark.frosk.strategies.indicators.SessionVWAPIndicator;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * Momentum Breakout — "Nyhetsmomentumet" (price-action proxy edition)
 *
 * <p>Originally designed to fire on literal news headlines, this strategy now
 * identifies stocks exhibiting the same price behaviour a positive catalyst
 * produces: a gap-up OR a strong intraday move on above-average volume, with
 * price holding above session VWAP and a rising EMA trend. No external news
 * feed is required — price action is the proxy.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>GapOrMomentum: opening gap &gt; 1.5% OR close &gt; prev-day close × 1.02</li>
 *   <li>VolumeConfirmed: volume &gt; EMA(20) × 1.5 — institutional participation</li>
 *   <li>PriceAboveVWAP: close &gt; session VWAP — buying pressure sustained</li>
 *   <li>TrendConfirmed: EMA(9) &gt; EMA(21) — short-term momentum pointing up</li>
 *   <li>HedgeIndex &le; frosk.intraday.hedge.max.score — no entries in strong risk-off</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>ATR trailing stop: 2.0 × ATR(14) below the highest close since entry</li>
 *   <li>EMA(9) crosses below EMA(21) — momentum reversal</li>
 *   <li>Max 16 bars held (≈ 4 hours on 15-min bars)</li>
 * </ul>
 */
@Component
@Slf4j
public class NewsBreakoutIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {

    private static final double GAP_UP_MIN_PCT    = 1.5;  // Opening gap threshold (%)
    private static final double MOMENTUM_FACTOR   = 1.02; // +2% intraday from prev close
    private static final int    VOLUME_EMA_PERIOD = 20;
    private static final double VOLUME_FACTOR     = 1.5;  // 50% above volume EMA
    private static final int    EMA_FAST          = 9;
    private static final int    EMA_SLOW          = 21;
    private static final int    ATR_PERIOD        = 14;
    private static final double ATR_TRAIL_MULT    = 2.0;  // chandelier exit: 2 × ATR(14)
    private static final int    MAX_BARS_HELD     = 16;   // ≈ 4 h on 15-min bars

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Value("${frosk.intraday.hedge.max.score:9}")
    private int intradayHedgeMaxScore;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator       close        = new ClosePriceIndicator(series);
        SessionVWAPIndicator      vwap         = new SessionVWAPIndicator(series);
        VolumeIndicator           volume       = new VolumeIndicator(series);
        EMAIndicator              volumeEma    = new EMAIndicator(volume, VOLUME_EMA_PERIOD);
        TransformIndicator        volThresh    = TransformIndicator.multiply(volumeEma, VOLUME_FACTOR);
        EMAIndicator              emaFast      = new EMAIndicator(close, EMA_FAST);
        EMAIndicator              emaSlow      = new EMAIndicator(close, EMA_SLOW);
        GapPercentIndicator       gapPct       = new GapPercentIndicator(series);
        PreviousDayCloseIndicator prevClose    = new PreviousDayCloseIndicator(series);
        TransformIndicator        momentumLine = TransformIndicator.multiply(prevClose, MOMENTUM_FACTOR);

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "sessionVwap");
        setIndicatorValues(volumeEma, "volumeEma20");
        setIndicatorValues(emaFast, "ema9");
        setIndicatorValues(emaSlow, "ema21");
        setIndicatorValues(gapPct, "gapPct");

        // ── Entry ─────────────────────────────────────────────────────────
        // Gap-up > 1.5% at open OR price has climbed > 2% above prev-day close intraday
        Rule gapUp          = new OverIndicatorRule(gapPct, DoubleNum.valueOf(GAP_UP_MIN_PCT));
        Rule intradayMove   = new OverIndicatorRule(close, momentumLine);
        Rule gapOrMomentum  = gapUp.or(intradayMove);

        Rule volumeConfirmed = new OverIndicatorRule(volume, volThresh);
        Rule priceAboveVwap  = new OverIndicatorRule(close, vwap);
        Rule trendUp         = new OverIndicatorRule(emaFast, emaSlow);
        Rule riskOn          = new HedgeIndexMaxScoreRule(series, hedgeIndexService, intradayHedgeMaxScore);

        Rule entryRule = gapOrMomentum.and(volumeConfirmed).and(priceAboveVwap).and(trendUp).and(riskOn);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule atrTrail = new AtrTrailingStopRule(series, ATR_PERIOD, ATR_TRAIL_MULT);
        Rule emaCross = new CrossedDownIndicatorRule(emaFast, emaSlow);
        Rule timeExit = new MaxBarsHeldRule(MAX_BARS_HELD);

        Rule exitRule = atrTrail.or(emaCross).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
