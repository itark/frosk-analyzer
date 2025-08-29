package nu.itark.frosk.strategies;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.hedge.*;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HighLanderStrategy extends AbstractStrategy implements IIndicatorValue {
    private final HedgeIndexStrategy hedgeIndexStrategy;
    private final BetaStrategy betaStrategy;
    private final YoYRevenueGrowthStrategy yoYRevenueGrowthStrategy;
    private final PEGRatioStrategy pegRatioStrategy;
    private final GoldenCrossRelativeStrengthStrategy goldenCrossRelativeStrengthStrategy;

    /**
     * Builds a composite trading strategy by combining hedge index and beta strategies.
     *
     * <ul>
     *   <li>Hedge index strategy - provides market hedging signals</li>
     *   <li>Beta strategy - provides beta-based trading signals</li>
     *   <li>Uses logical AND operation to combine both strategies</li>
     *   <li>Both conditions from both strategies must be satisfied for trades to execute</li>
     *   @todo Price above 50D & 200D MA
     *   @todo Outperformance vs. SPY over 3 months
     *   VVIX > 110 and rising
     *   SKEW > 140
     *   SDEX > 110 or rising fast
       * </ul>
     *
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li>Sets the inherent exit rule from the superclass</li>
     *   <li>Clears any existing indicator values</li>
     *   <li>Validates the input series for null values</li>
     *   <li>Assigns the series to the superclass field</li>
     *   <li>Builds and combines the hedge index and beta strategies</li>
     * </ul>
     *
     * @param series the bar series containing historical price data used to build the strategy.
     *               Must not be null and should contain sufficient data points for the
     *               underlying strategies to function properly.
     *
     * @return a composite {@link Strategy} that combines hedge index and beta strategies
     *         using AND logic, meaning both strategies must signal before executing trades
     *
     * @throws IllegalArgumentException if the series parameter is null
     *
     * @see Strategy
     * @see BarSeries
     *
     * @since 1.0
     */
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null, for:"+series.getName());
        }
        super.barSeries = series;

        Strategy strategy = hedgeIndexStrategy.buildStrategy(series)
                            .and(betaStrategy.buildStrategy(series))
                            .and(pegRatioStrategy.buildStrategy(series))
                            .and(goldenCrossRelativeStrengthStrategy.buildStrictGoldenCrossStrategy(series))
                            .and(yoYRevenueGrowthStrategy.buildStrategy(series));
        return strategy;
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
