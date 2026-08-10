package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.GapPercentIndicator;
import nu.itark.frosk.strategies.rules.AtrTrailingStopRule;
import nu.itark.frosk.strategies.rules.HedgeIndexMaxScoreRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * Daily-bar variant of {@link NewsBreakoutIntradayStrategy} — same gap +
 * volume + trend logic, translated to one bar per day.
 *
 * <p>Not a validated strategy yet: this is a first-pass backtest candidate.
 * Run {@code mvn test -Dtest=StrategyComparisonReportIT#dailyReport
 * -Dfrosk.compare.strategies=NewsBreakoutStrategy} before drawing any
 * conclusion — if trade count or SQN look thin, that is itself the answer.
 *
 * <h3>What changed from the intraday version, and why</h3>
 * <ul>
 *   <li>{@code SessionVWAPIndicator} has no daily equivalent — a session VWAP
 *   is defined by intraday bars within one trading day, and a daily bar series
 *   has exactly one bar per day. Replaced with close &gt; SMA(10): a two-week
 *   short-term support check standing in for "buying pressure held through the
 *   session".</li>
 *   <li>{@code GapPercentIndicator} / {@code PreviousDayCloseIndicator} needed
 *   no change — both already walk bar timestamps by calendar day, so on a
 *   genuinely daily series they degrade correctly to the plain
 *   yesterday-vs-today comparison.</li>
 *   <li>Holding period: 16 bars (~4h on 15-min bars) has no meaningful
 *   translation, so this is re-derived as 12 trading days (~2.5 weeks) — long
 *   enough for a daily breakout to play out, short enough to stay a breakout
 *   trade rather than a trend-following one.</li>
 *   <li>HedgeIndex gate uses the standard daily entry threshold (7, matching
 *   {@code frosk.hedge.criteria.risk.threshold}) rather than the intraday
 *   version's looser 9 — the intraday version re-evaluates every 15 minutes
 *   and exits fast; a daily position sits through more regime risk per trade.</li>
 * </ul>
 *
 * <h3>Measured performance — read this before trusting it</h3>
 * First pass (gap OR +2% momentum move, 925 trades) lost -0.84%/trade, SQN
 * -1.23. Diagnosed with a one-off breakdown (see git history /
 * NewsBreakoutStrategyDiagnosticIT): the two entry legs are not equal —
 * gap-up entries alone averaged +3.54%/trade (133 trades, 14% of volume)
 * while the momentum-move leg averaged -1.58%/trade and made up the other
 * 86%, dragging the blended result negative. Exit-side, positions that
 * survived to the 12-bar time exit averaged +8.2%; positions stopped out by
 * the ATR trail or EMA cross had already given back -6% to -9.4% by the time
 * the stop fired, i.e. the stop confirms damage rather than limiting it.
 * The momentum-move leg has been removed below on that evidence — gap-only,
 * not yet re-measured. Do not add it back, and do not tighten the stops,
 * without a fresh run of the comparison report first.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>GapUp: opening gap &gt; 1.5% — the momentum-move alternative
 *   (close &gt; prev-day close × 1.02) was in the first pass and is not
 *   here; see "Measured performance" above</li>
 *   <li>VolumeConfirmed: volume &gt; EMA(20) × 1.5 — institutional participation</li>
 *   <li>PriceAboveShortSma: close &gt; SMA(10) — short-term support held</li>
 *   <li>TrendConfirmed: EMA(9) &gt; EMA(21) — short-term momentum pointing up</li>
 *   <li>HedgeIndex &le; frosk.newsbreakout.hedge.max.score — no entries in risk-off</li>
 * </ul>
 *
 * <h3>Exit rules (first satisfied wins)</h3>
 * <ul>
 *   <li>ATR trailing stop: 2.0 × ATR(14) below the highest close since entry</li>
 *   <li>EMA(9) crosses below EMA(21) — momentum reversal</li>
 *   <li>Max 12 bars held (~2.5 weeks on daily bars)</li>
 * </ul>
 */
@Component
@Slf4j
public class NewsBreakoutStrategy extends AbstractStrategy implements IIndicatorValue {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final double GAP_UP_MIN_PCT    = 1.5;  // Opening gap threshold (%)
    private static final int    VOLUME_EMA_PERIOD = 20;
    private static final double VOLUME_FACTOR     = 1.5;  // 50% above volume EMA
    private static final int    SHORT_SMA_PERIOD  = 10;   // VWAP stand-in: 2-week support
    private static final int    EMA_FAST          = 9;
    private static final int    EMA_SLOW          = 21;
    private static final int    ATR_PERIOD        = 14;
    private static final double ATR_TRAIL_MULT    = 2.0;  // chandelier exit: 2 × ATR(14)
    private static final int    MAX_BARS_HELD     = 12;   // ~2.5 weeks on daily bars

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Value("${frosk.newsbreakout.hedge.max.score:7}")
    private int hedgeMaxScore;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator       close        = new ClosePriceIndicator(series);
        SMAIndicator              shortSma     = new SMAIndicator(close, SHORT_SMA_PERIOD);
        VolumeIndicator           volume       = new VolumeIndicator(series);
        EMAIndicator              volumeEma    = new EMAIndicator(volume, VOLUME_EMA_PERIOD);
        TransformIndicator        volThresh    = TransformIndicator.multiply(volumeEma, VOLUME_FACTOR);
        EMAIndicator              emaFast      = new EMAIndicator(close, EMA_FAST);
        EMAIndicator              emaSlow      = new EMAIndicator(close, EMA_SLOW);
        GapPercentIndicator       gapPct       = new GapPercentIndicator(series);

        setIndicatorValues(close, "close");
        setIndicatorValues(shortSma, "shortSma10");
        // volumeEma20 deliberately NOT persisted here: raw share-volume EMAs on
        // liquid securities routinely exceed strat_indicator_value.value_'s
        // NUMERIC(12,6) precision (6 digits before the decimal) and throw a
        // DataIntegrityViolationException that aborts the whole strategy run.
        // Same class of bug AbstractStrategy.liquidityRule() already warns about
        // for the turnover indicator — confirmed live on ATCO-B.ST (EMA hit
        // 1,066,552) when frosk.strategy.persist.indicator.values was briefly
        // turned on to test the chart overlay for this strategy.
        setIndicatorValues(emaFast, "ema9");
        setIndicatorValues(emaSlow, "ema21");
        setIndicatorValues(gapPct, "gapPct");

        // ── Entry ─────────────────────────────────────────────────────────
        // Gap-up > 1.5% at open. The momentum-move alternative (close > 1.02 ×
        // prev-day close) was removed here — see "Measured performance" above.
        Rule gapUp = new OverIndicatorRule(gapPct, DoubleNum.valueOf(GAP_UP_MIN_PCT));

        Rule volumeConfirmed  = new OverIndicatorRule(volume, volThresh);
        Rule priceAboveShortSma = new OverIndicatorRule(close, shortSma);
        Rule trendUp           = new OverIndicatorRule(emaFast, emaSlow);
        Rule riskOn             = new HedgeIndexMaxScoreRule(series, hedgeIndexService, hedgeMaxScore);

        Rule entryRule = gapUp.and(volumeConfirmed).and(priceAboveShortSma).and(trendUp).and(riskOn);

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
