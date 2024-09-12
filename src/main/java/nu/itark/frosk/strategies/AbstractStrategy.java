package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.TrailingStopLossRule;

@Slf4j
@Component
public abstract  class AbstractStrategy {
    private Rule exitRule;
    BarSeries barSeries;
    BarSeries barSeriesWithForecast;

    BarSeriesService barSeriesService = new BarSeriesService();

    public Boolean inherentExitRule;

    @Autowired
    private TradingAccountService tradingAccountService;

    void setInherentExitRule() {
        inherentExitRule= tradingAccountService.getActiveTradingAccount().getAccountType().getInherentExitRule();
    }

    Rule exitRule() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(barSeries);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 2);

        exitRule = pSarIsFallingRule
               // .or(new StopLossRule(closePrice, 2));
               .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));


      //  exitRule = new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2));

        return exitRule;
    }

}
