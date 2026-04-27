package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.HedgeIndexService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Tiered HedgeIndex entry rule for Månadsportföljen.
 * <ul>
 *   <li>Score 0–3: entry allowed (Strong Risk-On)</li>
 *   <li>Score 4–7: entry allowed (Cautious — portfolio sizing handled externally)</li>
 *   <li>Score 8+: entry blocked (Defensive / Risk-Off)</li>
 * </ul>
 */
public class HedgeIndexTieredRule extends AbstractRule {
    private final BarSeries barSeries;
    private final HedgeIndexService hedgeIndexService;
    private final int maxScore;

    public HedgeIndexTieredRule(BarSeries barSeries, HedgeIndexService hedgeIndexService, int maxScore) {
        this.barSeries = barSeries;
        this.hedgeIndexService = hedgeIndexService;
        this.maxScore = maxScore;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int score = hedgeIndexService.getScore(barSeries.getBar(index).getEndTime());
        return score <= maxScore;
    }
}
