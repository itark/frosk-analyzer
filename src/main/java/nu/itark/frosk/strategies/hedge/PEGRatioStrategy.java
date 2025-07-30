package nu.itark.frosk.strategies.hedge;

import lombok.Data;
import nu.itark.frosk.strategies.indicators.MultipliedIndicator;
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

import java.util.HashMap;
import java.util.Map;

/**
 * From https://claude.ai/
 */
public class PEGRatioStrategy {
    // PEG ratio thresholds
    private static final double EXCELLENT_PEG = 0.5;
    private static final double GOOD_PEG = 1.0;
    private static final double FAIR_PEG = 1.5;
    private static final double EXPENSIVE_PEG = 2.0;

    // Strategy parameters
    private final int smaShortPeriod;
    private final int smaLongPeriod;
    private final int rsiPeriod;
    private final double rsiOverbought;
    private final double rsiOversold;

    // Fundamental data storage
    private final Map<String, FundamentalData> fundamentalDataMap;

    public PEGRatioStrategy() {
        this(10, 20, 14, 70, 30);
    }

    public PEGRatioStrategy(int smaShortPeriod, int smaLongPeriod, int rsiPeriod,
                            double rsiOverbought, double rsiOversold) {
        this.smaShortPeriod = smaShortPeriod;
        this.smaLongPeriod = smaLongPeriod;
        this.rsiPeriod = rsiPeriod;
        this.rsiOverbought = rsiOverbought;
        this.rsiOversold = rsiOversold;
        this.fundamentalDataMap = new HashMap<>();
    }

    /**
     * Fundamental data holder
     */
    @Data
    public static class FundamentalData {
        private final double pegRatio;
        private final double forwardPE;
        private final double trailingEps;
        private final double forwardEps;
        private final double bookValue;
        private final double priceToBook;

        public FundamentalData(double pegRatio, double forwardPE, double trailingEps,
                               double forwardEps, double bookValue, double priceToBook) {
            this.pegRatio = pegRatio;
            this.forwardPE = forwardPE;
            this.trailingEps = trailingEps;
            this.forwardEps = forwardEps;
            this.bookValue = bookValue;
            this.priceToBook = priceToBook;
        }

        /**
         * Calculate estimated PEG if not provided
         */
        public double getEstimatedPegRatio() {
            if (pegRatio > 0) {
                return pegRatio;
            }

            // Calculate 1-year growth rate estimate
            if (trailingEps > 0 && forwardEps > 0 && forwardPE > 0) {
                double growthRate = ((forwardEps - trailingEps) / trailingEps) * 100;
                if (growthRate > 0) {
                    return forwardPE / growthRate;
                }
            }

            return Double.MAX_VALUE; // Invalid PEG
        }
    }

    /**
     * Add fundamental data for a symbol
     */
    public void addFundamentalData(String symbol, FundamentalData data) {
        fundamentalDataMap.put(symbol, data);
    }

    /**
     * Create the complete PEG-based trading strategy
     */
    public Strategy createStrategy(BarSeries series, String symbol) {
        if (series == null || series.getBarCount() == 0) {
            throw new IllegalArgumentException("Invalid bar series");
        }

        FundamentalData fundamentalData = fundamentalDataMap.get(symbol);
        if (fundamentalData == null) {
            throw new IllegalArgumentException("No fundamental data found for symbol: " + symbol);
        }

        // Technical indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator smaShort = new SMAIndicator(closePrice, smaShortPeriod);
        SMAIndicator smaLong = new SMAIndicator(closePrice, smaLongPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        // Volume indicator for confirmation
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);

        // Entry Rules
        Rule entryRule = createEntryRule(series, symbol, closePrice, smaShort, smaLong, rsi, volume, avgVolume);

        // Exit Rules
        Rule exitRule = createExitRule(series, symbol, closePrice, smaShort, smaLong, rsi);

        return new BaseStrategy("PEG-Ratio-Strategy-" + symbol, entryRule, exitRule);
    }

    /**
     * Create entry rules combining PEG analysis with technical signals
     */
    private Rule createEntryRule(BarSeries series, String symbol, ClosePriceIndicator closePrice,
                                 SMAIndicator smaShort, SMAIndicator smaLong, RSIIndicator rsi,
                                 VolumeIndicator volume, SMAIndicator avgVolume) {

        FundamentalData data = fundamentalDataMap.get(symbol);
        double pegRatio = data.getEstimatedPegRatio();

        // Fundamental rules based on PEG ratio
        Rule fundamentalRule;
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
        } else {
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
    private Rule createExitRule(BarSeries series, String symbol, ClosePriceIndicator closePrice,
                                SMAIndicator smaShort, SMAIndicator smaLong, RSIIndicator rsi) {

        FundamentalData data = fundamentalDataMap.get(symbol);
        double pegRatio = data.getEstimatedPegRatio();

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

    /**
     * Get PEG-based position sizing recommendation
     */
    public double getPositionSizeMultiplier(String symbol) {
        FundamentalData data = fundamentalDataMap.get(symbol);
        if (data == null) return 0.5; // Conservative default

        double pegRatio = data.getEstimatedPegRatio();

        if (pegRatio <= EXCELLENT_PEG) {
            return 1.5; // Larger position for excellent value
        } else if (pegRatio <= GOOD_PEG) {
            return 1.0; // Standard position
        } else if (pegRatio <= FAIR_PEG) {
            return 0.7; // Smaller position
        } else if (pegRatio <= EXPENSIVE_PEG) {
            return 0.3; // Very small position
        } else {
            return 0.0; // No position for very expensive stocks
        }
    }

    /**
     * Evaluate stock attractiveness based on PEG and other fundamentals
     */
    public String evaluateStock(String symbol) {
        FundamentalData data = fundamentalDataMap.get(symbol);
        if (data == null) return "No data available";

        double pegRatio = data.getEstimatedPegRatio();
        StringBuilder evaluation = new StringBuilder();

        evaluation.append("Stock Evaluation for ").append(symbol).append(":\n");
        evaluation.append("PEG Ratio: ").append(String.format("%.2f", pegRatio)).append("\n");

        if (pegRatio <= EXCELLENT_PEG) {
            evaluation.append("Rating: EXCELLENT VALUE - Strong Buy Candidate\n");
            evaluation.append("Recommendation: Large position, aggressive entry\n");
        } else if (pegRatio <= GOOD_PEG) {
            evaluation.append("Rating: GOOD VALUE - Buy Candidate\n");
            evaluation.append("Recommendation: Standard position, wait for technical confirmation\n");
        } else if (pegRatio <= FAIR_PEG) {
            evaluation.append("Rating: FAIRLY VALUED - Hold/Small Buy\n");
            evaluation.append("Recommendation: Small position, conservative entry\n");
        } else if (pegRatio <= EXPENSIVE_PEG) {
            evaluation.append("Rating: EXPENSIVE - Avoid/Sell\n");
            evaluation.append("Recommendation: Very small position or avoid\n");
        } else {
            evaluation.append("Rating: VERY EXPENSIVE - Strong Sell\n");
            evaluation.append("Recommendation: No position, consider shorting\n");
        }

        // Additional fundamental insights
        evaluation.append("\nAdditional Metrics:\n");
        evaluation.append("Forward P/E: ").append(String.format("%.2f", data.getForwardPE())).append("\n");
        evaluation.append("Price-to-Book: ").append(String.format("%.2f", data.getPriceToBook())).append("\n");

        if (data.getForwardEps() > data.getTrailingEps()) {
            double growth = ((data.getForwardEps() - data.getTrailingEps()) / data.getTrailingEps()) * 100;
            evaluation.append("Est. EPS Growth: ").append(String.format("%.1f%%", growth)).append("\n");
        }

        return evaluation.toString();
    }

    /**
     * Example usage and testing
     */
    public static void main(String[] args) {
        // Example usage
        PEGRatioStrategy strategy = new PEGRatioStrategy();

        // Add sample fundamental data
        FundamentalData sampleData = new FundamentalData(
                0.8,    // PEG ratio
                25.0,   // Forward P/E
                2.50,   // Trailing EPS
                3.00,   // Forward EPS
                15.0,   // Book value
                2.5     // Price-to-book
        );

        strategy.addFundamentalData("AAPL", sampleData);

        // Print evaluation
        System.out.println(strategy.evaluateStock("AAPL"));
        System.out.println("Position Size Multiplier: " + strategy.getPositionSizeMultiplier("AAPL"));
    }



}
