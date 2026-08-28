package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.repo.NewsRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.strategies.indicators.NewsIndicator;
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
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanIndicatorRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.util.List;

/**
 * News-driven momentum breakout — the genuinely news-fed sibling of
 * {@link NewsBreakoutIntradayStrategy}.
 *
 * <p>{@link NewsBreakoutIntradayStrategy} deliberately does <em>not</em> read
 * a live news feed (see its javadoc: a real-news-driven version caused a
 * {@code DataIntegrityViolationException} in production on {@code ATCO-B.ST}
 * via a persisted volume-EMA indicator, an unrelated indicator that happened
 * to ship in the same strategy). This strategy takes the real-news path
 * instead — {@link NewsIndicator}, backed by {@link NewsRepository}
 * rows polled from Nasdaq Nordic RSS — layered as an additional entry filter
 * on top of the same VWAP/EMA-trend logic, so a signal requires both price
 * action <em>and</em> a qualifying recent news article for that ticker.
 *
 * <p>Not registered in the portfolio and not wired into
 * {@code NewsBreakoutIntradayStrategy}; it is a separate, independently
 * evaluable strategy so the price-action proxy that is already live keeps
 * running unchanged.
 *
 * <h3>Entry rules (all must be true)</h3>
 * <ul>
 *   <li>PriceAboveVWAP: close &gt; session VWAP — buying pressure sustained</li>
 *   <li>TrendConfirmed: EMA(9) &gt; EMA(21) — short-term momentum pointing up</li>
 *   <li>HedgeIndex &le; frosk.intraday.hedge.max.score — no entries in strong risk-off</li>
 *   <li>NewsPresent: an RSS article with |sentimentScore| &ge;
 *       frosk.newsdriven.rss.min.abs.score for this ticker within
 *       frosk.newsdriven.rss.lookback.minutes</li>
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
public class NewsBreakoutRssIntradayStrategy extends AbstractStrategy implements IIndicatorValue, IntradayStrategy {
    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int EMA_FAST       = 9;
    private static final int EMA_SLOW       = 21;
    private static final int ATR_PERIOD     = 14;
    private static final double ATR_TRAIL_MULT = 2.0;
    private static final int MAX_BARS_HELD  = 16;

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Value("${frosk.intraday.hedge.max.score:9}")
    private int intradayHedgeMaxScore;

    @Value("${frosk.newsdriven.rss.lookback.minutes:60}")
    private int newsLookbackMinutes;

    @Value("${frosk.newsdriven.rss.min.abs.score:2}")
    private int newsMinAbsScore;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();

        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        String ticker = resolveTicker(series);

        ClosePriceIndicator  close   = new ClosePriceIndicator(series);
        SessionVWAPIndicator vwap    = new SessionVWAPIndicator(series);
        EMAIndicator          emaFast = new EMAIndicator(close, EMA_FAST);
        EMAIndicator          emaSlow = new EMAIndicator(close, EMA_SLOW);
        NewsIndicator          news    = new NewsIndicator(series, newsRepository,
                ticker, newsLookbackMinutes, newsMinAbsScore);

        setIndicatorValues(close, "close");
        setIndicatorValues(vwap, "sessionVwap");
        setIndicatorValues(emaFast, "ema9");
        setIndicatorValues(emaSlow, "ema21");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule priceAboveVwap = new OverIndicatorRule(close, vwap);
        Rule trendUp        = new OverIndicatorRule(emaFast, emaSlow);
        Rule riskOn          = new HedgeIndexMaxScoreRule(series, hedgeIndexService, intradayHedgeMaxScore);
        Rule newsPresent     = new BooleanIndicatorRule(news);

        Rule entryRule = priceAboveVwap.and(trendUp).and(riskOn).and(newsPresent);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule atrTrail = new AtrTrailingStopRule(series, ATR_PERIOD, ATR_TRAIL_MULT);
        Rule emaCross = new CrossedDownIndicatorRule(emaFast, emaSlow);
        Rule timeExit = new MaxBarsHeldRule(MAX_BARS_HELD);

        Rule exitRule = atrTrail.or(emaCross).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /** {@code series.getName()} is the security id (see {@code IntradayDataService}), not the ticker. */
    private String resolveTicker(BarSeries series) {
        try {
            Long securityId = Long.valueOf(series.getName());
            Security security = securityRepository.findById(securityId).orElse(null);
            return security != null ? security.getName() : null;
        } catch (NumberFormatException e) {
            log.warn("NewsBreakoutRssIntradayStrategy: series name '{}' is not a security id", series.getName());
            return null;
        }
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
