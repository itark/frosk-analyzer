package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.AbstractStrategy;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;

import java.util.List;

@Component
/**
 *
 The PEG Ratio (Price/Earnings to Growth ratio) on Yahoo Finance measures a stock’s valuation relative
 to its earnings growth, providing a more balanced view than the P/E ratio alone.

 The PEG ratio on Yahoo Finance helps investors determine if a stock is overpriced or underpriced relative to its earnings growth,
 making it a valuable tool for growth investing.

 PEG = 1: Stock is fairly valued given its growth rate.
 PEG < 1: May be undervalued relative to growth potential.
 PEG > 1: May be overvalued or priced for high expectations.

 Stock with Forward P/E = 20
 Expected EPS growth = 25% per year
 PEG = 20 / 25 = 0.8 → Suggests undervaluation for its growth
 */
@Deprecated
public class SimplePEGRatioStrategy extends AbstractStrategy implements IIndicatorValue  {

    @Value("${frosk.hedge.criteria.pegratio.threshold}")
    private Double pegRatioThreshold;


    /**
     * PEG Ratio Strategy: Buy if PEG ratio < 1.5
     * Note: PEG ratio is retrieved externally and applied as a static filter
     */
    public Strategy buildStrategy(BarSeries series) {
        Double pegRatio =  getPEGRatio(series.getName());
         Rule entryRule;
        Rule exitRule;
        if (pegRatio > 0 && pegRatio < pegRatioThreshold) {
            // PEG is favorable → enable strategy
            entryRule = BooleanRule.TRUE;  // Always enter (subject to additional filters if needed)
            exitRule = BooleanRule.FALSE;  // Never exit based on PEG alone
        } else {
            // PEG too high → no trade
            entryRule = BooleanRule.FALSE;
            exitRule = BooleanRule.TRUE;   // Always exit if in position
        }
        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

}
