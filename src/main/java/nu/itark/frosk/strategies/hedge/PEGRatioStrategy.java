package nu.itark.frosk.strategies.hedge;

import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import nu.itark.frosk.strategies.indicators.MultipliedIndicator;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * From https://claude.ai/
 *
 * What is PEG Ratio?
 *
 * PEG = (P/E Ratio) / (EPS Growth Rate)
 *
 * P/E: Price / Earnings
 *
 * EPS Growth Rate: Usually forward-looking (% expected annual growth)
 *
 *
 * PEG = (P/E ratio) / (5Y EPS growth in %)
 * For example, if peRatio = 30 and expected EPS growth = 25% → PEG = 30 / 25 = 1.2.
 */
@Component
public class PEGRatioStrategy extends AbstractStrategy implements IIndicatorValue  {
    // PEG ratio thresholds
    private static final double EXCELLENT_PEG = 0.5;
    private static final double GOOD_PEG = 1.0;
    private static final double FAIR_PEG = 1.5;
    private static final double EXPENSIVE_PEG = 2.0;

    // Strategy parameters
    private final int smaShortPeriod = 10;
    private final int smaLongPeriod = 20;
    private final int rsiPeriod = 14;
    private final double rsiOverbought = 70;
    private final double rsiOversold= 30;

    public Strategy buildStrategy(BarSeries series) {
        Double pegRatio =  getPEGRatio(series.getName());
        // Technical indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator smaShort = new SMAIndicator(closePrice, smaShortPeriod);
        SMAIndicator smaLong = new SMAIndicator(closePrice, smaLongPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        // Volume indicator for confirmation
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);

        // Entry Rules
        Rule entryRule = createEntryRule(series, pegRatio, closePrice, smaShort, smaLong, rsi, volume, avgVolume);

        // Exit Rules
        Rule exitRule = createExitRule(series, pegRatio, closePrice, smaShort, smaLong, rsi);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /**
     * Create entry rules combining PEG analysis with technical signals
     */
    private Rule createEntryRule(BarSeries series, Double pegRatio, ClosePriceIndicator closePrice,
                                 SMAIndicator smaShort, SMAIndicator smaLong, RSIIndicator rsi,
                                 VolumeIndicator volume, SMAIndicator avgVolume) {

        // Fundamental rules based on PEG ratio
        Rule fundamentalRule = BooleanRule.FALSE;
        if (pegRatio <= EXCELLENT_PEG) {
            // Excellent PEG - more aggressive entry
            fundamentalRule = new BooleanRule(true); // Always allow entry on technical signals
        } else if (pegRatio <= GOOD_PEG) {
            // Good PEG - standard entry with technical confirmation
            fundamentalRule = new OverIndicatorRule(smaShort, smaLong); // Uptrend required
        } else if (pegRatio <= FAIR_PEG) {
            // Fair PEG - conservative entry, need strong technical signals
            fundamentalRule = new OverIndicatorRule(smaShort, smaLong)
                    .and(new OverIndicatorRule(closePrice, smaLong)); // Price above long-term trend
        } else if (pegRatio <= EXPENSIVE_PEG)  {
            // Expensive PEG - very conservative, only on strong dips
            fundamentalRule = new UnderIndicatorRule(rsi, series.numOf(rsiOversold))
                    .and(new OverIndicatorRule(smaShort, smaLong)); // Oversold in uptrend
        }

        // Create volume threshold (120% of average volume)
        MultipliedIndicator volumeThreshold = new MultipliedIndicator(avgVolume, series.numOf(1.2));

        // Technical entry signals
        Rule technicalEntry = new OverIndicatorRule(smaShort, smaLong) // Short MA above Long MA
                .and(new UnderIndicatorRule(rsi, series.numOf(rsiOverbought))) // Not overbought
                .and(new OverIndicatorRule(volume, volumeThreshold)); // Volume confirmation

        // Price momentum rule
        Rule momentumRule = new OverIndicatorRule(closePrice, new SMAIndicator(closePrice, 5));

        return fundamentalRule.and(technicalEntry).and(momentumRule);
    }

    /**
     * Create exit rules
     */
    private Rule createExitRule(BarSeries series, Double pegRatio, ClosePriceIndicator closePrice,
                                SMAIndicator smaShort, SMAIndicator smaLong, RSIIndicator rsi) {

        // Standard technical exit signals
        Rule technicalExit = new UnderIndicatorRule(smaShort, smaLong) // Short MA below Long MA
                .or(new OverIndicatorRule(rsi, series.numOf(rsiOverbought))); // Overbought

        // PEG-based profit taking
        Rule pegBasedExit;
        if (pegRatio <= EXCELLENT_PEG) {
            // Hold longer for excellent PEG stocks
            pegBasedExit = new OverIndicatorRule(rsi, series.numOf(80)); // Very overbought
        } else if (pegRatio <= GOOD_PEG) {
            // Standard exit for good PEG stocks
            pegBasedExit = new OverIndicatorRule(rsi, series.numOf(rsiOverbought));
        } else {
            // Quick profit taking for expensive stocks
            pegBasedExit = new OverIndicatorRule(rsi, series.numOf(65)); // Earlier exit
        }

        // Stop loss rule - price drops below long-term MA
        Rule stopLossRule = new UnderIndicatorRule(closePrice, smaLong);

        return technicalExit.or(pegBasedExit).or(stopLossRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

}
