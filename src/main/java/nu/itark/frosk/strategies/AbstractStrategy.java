package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.rules.StopLossRule;
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
    protected  BarSeries barSeries;
    BarSeries barSeriesWithForecast;
    public Boolean inherentExitRule;

    @Autowired
    private TradingAccountService tradingAccountService;

    @Autowired
    private SecurityRepository securityRepository;

    void setInherentExitRule() {
        inherentExitRule= tradingAccountService.getActiveTradingAccount().getAccountType().getInherentExitRule();
    }

    Rule exitRule() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(barSeries);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 2);

        exitRule = pSarIsFallingRule
                .or(new StopLossRule(closePrice, 2))
               .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));

        return exitRule;
    }

    public Double getPEGRatio(String symbol) {
        final Security security = securityRepository.findByName(symbol);
        return security.getPegRatio() != null ? security.getPegRatio() : Double.valueOf(0.0);
    }

    public Double getBeta(String symbol) {
        final Security security = securityRepository.findByName(symbol);
        return security.getBeta() != null ? security.getBeta() : Double.valueOf(0.0);
    }


    public Double getYoYGrowth(String symbol) {
        final Security security = securityRepository.findByName(symbol);
        return security.getYoyGrowth() != null ? security.getYoyGrowth() : Double.valueOf(0.0);
    }


}
