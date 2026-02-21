package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OrRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * TA4j Strategy based on RecommendationTrend data
 * <p>
 * Entry conditions:
 * - Bullish percentage > 60% AND recommendation score > 3.5
 * - OR bullish percentage increasing over last 3 periods
 * <p>
 * Exit conditions:
 * - Bearish percentage > 40% OR recommendation score < 3.0
 * - OR bullish percentage decreasing significantly
 */
@Component
public class RecommendationTrendStrategy extends AbstractStrategy implements IIndicatorValue {

    private static final double BULLISH_THRESHOLD = 60.0;
    private static final double BEARISH_THRESHOLD = 40.0;
    private static final double SCORE_BUY_THRESHOLD = 3.5;
    private static final double SCORE_SELL_THRESHOLD = 3.0;

    /**
     * Build a TA4j strategy from recommendation trend data
     */
    public Strategy buildStrategy(BarSeries seriesRaw) {
        List<RecommendationTrend> trends = getRecommendationTrends(seriesRaw.getName());

        BarSeries series = createBarSeriesFromRecommendations(seriesRaw.getName(), trends);

        // Create custom indicators
        RecommendationScoreIndicator scoreIndicator = new RecommendationScoreIndicator(series);
        BullishPercentageIndicator bullishIndicator = new BullishPercentageIndicator(series);
        BearishPercentageIndicator bearishIndicator = new BearishPercentageIndicator(series);

        // Entry rules
        Rule scoreEntryRule = new OverIndicatorRule(scoreIndicator, SCORE_BUY_THRESHOLD);
        Rule bullishEntryRule = new OverIndicatorRule(bullishIndicator, BULLISH_THRESHOLD);
        Rule strongEntryRule = new AndRule(scoreEntryRule, bullishEntryRule);

        // Trend improvement rule (bullish % increasing)
        Rule trendImprovingRule = new BullishTrendImprovingRule(bullishIndicator, 3);

        Rule entryRule = new OrRule(strongEntryRule, trendImprovingRule);

        // Exit rules
        Rule scoreExitRule = new UnderIndicatorRule(scoreIndicator, SCORE_SELL_THRESHOLD);
        Rule bearishExitRule = new OverIndicatorRule(bearishIndicator, BEARISH_THRESHOLD);

        Rule exitRule = new OrRule(scoreExitRule, bearishExitRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /*
     */
/**
 * Convert RecommendationTrend entities to a TA4j BarSeries
 * Uses recommendation score as the "price" for compatibility with TA4j
 */

    public static BarSeries createBarSeriesFromRecommendations(
            String securitySymbol,
            List<RecommendationTrend> trends) {

        BarSeries series = new BaseBarSeriesBuilder().withName(securitySymbol).withNumTypeOf(DoubleNum.class).build();

        // Sort by period (assuming "-3m", "-2m", "-1m", "0m" format)
        trends.sort((a, b) -> comparePeriods(a.getPeriod(), b.getPeriod()));

        for (RecommendationTrend trend : trends) {
            // Use recommendation metrics as bar data
            // Open/High/Low/Close all use the score for simplicity
            double score = trend.getRecommendationScore();
            double bullish = trend.getBullishPercentage();
            double volume = trend.getTotalRecommendations();

            // Store additional data in the bar for indicator access
            ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.systemDefault());
            series.addBar(dateTime, score, score, score, score, volume);
        }

        return series;
    }


    /**
     * Compare period strings for sorting
     * "-3m" < "-2m" < "-1m" < "0m"
     */
    private static int comparePeriods(String p1, String p2) {
        int v1 = parsePeriodValue(p1);
        int v2 = parsePeriodValue(p2);
        return Integer.compare(v1, v2);
    }

    private static int parsePeriodValue(String period) {
        if (period.equals("0m")) return 0;
        return Integer.parseInt(period.replace("m", ""));
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return List.of();
    }

    /**
     * Custom indicator for recommendation score
     */
    static class RecommendationScoreIndicator extends CachedIndicator<Num> {

        public RecommendationScoreIndicator(BarSeries series) {
            super(series);
        }

        @Override
        protected Num calculate(int index) {
            BarSeries series = getBarSeries();
            int barCount = series.getBarCount();
            if (barCount == 0) {
                return numOf(0);
            }
            int safeIndex = Math.min(index, barCount - 1);
            // The close price represents the recommendation score
            return series.getBar(safeIndex).getClosePrice();
        }




        @Override
        public int getUnstableBars() {
            return getUnstableBars();
        }
    }

    /**
     * Custom indicator for bullish percentage
     * Stored in bar user data or calculated from volume
     */
    static class BullishPercentageIndicator extends CachedIndicator<Num> {

        public BullishPercentageIndicator(BarSeries series) {
            super(series);
        }

        @Override
        protected Num calculate(int index) {
            // This would ideally come from bar user data
            // For now, estimate from score: (score - 3) * 25 approximates percentage

            BarSeries series = getBarSeries();
            int barCount = series.getBarCount();
            if (barCount == 0) {
                return numOf(0);
            }
            int safeIndex = Math.min(index, barCount - 1);
            Num score = getBarSeries().getBar(safeIndex).getClosePrice();
            return score.minus(numOf(3)).multipliedBy(numOf(25));
        }

        @Override
        public int getUnstableBars() {
            return getUnstableBars();
        }
    }

    /**
     * Custom indicator for bearish percentage
     */
    static class BearishPercentageIndicator extends CachedIndicator<Num> {

        public BearishPercentageIndicator(BarSeries series) {
            super(series);
        }

        @Override
        protected Num calculate(int index) {
            // Estimate from score: (3 - score) * 25 approximates bearish %
            Num score = getBarSeries().getBar(index).getClosePrice();
            return numOf(3).minus(score).multipliedBy(numOf(25)).max(numOf(0));
        }

        @Override
        public int getUnstableBars() {
            return 0;
        }
    }

    /**
     * Custom rule to detect improving bullish trend
     */
    static class BullishTrendImprovingRule implements Rule {
        private final BullishPercentageIndicator indicator;
        private final int lookback;

        public BullishTrendImprovingRule(BullishPercentageIndicator indicator, int lookback) {
            this.indicator = indicator;
            this.lookback = lookback;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (index < lookback) {
                return false;
            }

            // Check if bullish % has been increasing over lookback period
            for (int i = 1; i < lookback; i++) {
                Num current = indicator.getValue(index - i + 1);
                Num previous = indicator.getValue(index - i);

                if (current.isLessThanOrEqual(previous)) {
                    return false;
                }
            }

            return true;
        }
    }
}