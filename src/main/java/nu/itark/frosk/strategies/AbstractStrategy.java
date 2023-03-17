package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.*;

@Slf4j
public abstract  class AbstractStrategy {
    private Rule exitRule;
    private BarSeries barSeries;

    @Value("${frosk.inherent.exitrule}")
    public boolean inherentExitRule = true;

    AbstractStrategy(BarSeries barSeries) {
        this.barSeries = barSeries;

    }
    Rule exitRule() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(barSeries);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 1);

        exitRule = pSarIsFallingRule
                .or(new StopLossRule(closePrice, 4))
                .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(4)));

        return exitRule;
    }

}
