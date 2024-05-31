package nu.itark.frosk.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.TrailingStopLossRule;

/**
 * REMOVE?
 */

public class ExitStrategy {

    public Rule exitRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(barSeries);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 2);

        Rule exitRule = pSarIsFallingRule
                // .or(new StopLossRule(closePrice, 2));
                .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));

        return exitRule;
    }


}
