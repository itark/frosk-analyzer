package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.HedgeIndexService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate satisfied while the HedgeIndex score in effect on the bar's day
 * is at most {@code maxScore}. Works on daily and intraday (15m) bars alike,
 * since the score lookup floors to the bar's calendar day.
 *
 * <p>Regime tiers: 0–3 strong risk-on, 4–7 cautious, 8+ defensive/blocked —
 * so {@code maxScore = 7} allows entries in risk-on and cautious regimes only.
 */
public class HedgeIndexMaxScoreRule extends AbstractRule {
    private final BarSeries barSeries;
    private final HedgeIndexService hedgeIndexService;
    private final int maxScore;

    public HedgeIndexMaxScoreRule(BarSeries barSeries, HedgeIndexService hedgeIndexService, int maxScore) {
        this.barSeries = barSeries;
        this.hedgeIndexService = hedgeIndexService;
        this.maxScore = maxScore;
    }

    /** This rule does not use the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return hedgeIndexService.getScoreForDay(barSeries.getBar(index).getEndTime()) <= maxScore;
    }
}
