package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.bot.bot.strategy.BasicCassandreStrategy;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.surus.math.AugmentedDickeyFuller;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.StopLossRule;
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

    public Boolean stationary;

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

        return exitRule;
    }


    //https://www.analyticsvidhya.com/blog/2021/06/statistical-tests-to-check-stationarity-in-time-series-part-1/#Augmented_Dickey-Fuller_Test
    private Boolean isStationary() {
        double[] closePrices = new double[barSeries.getBarCount()];
        double[] highPrices = new double[barSeries.getBarCount()];
        double[] lowPrices = new double[barSeries.getBarCount()];
        double[] openPrices = new double[barSeries.getBarCount()];
        double[] volumes = new double[barSeries.getBarCount()];

        for (int i = 0; i < barSeries.getBarCount(); i++) {
            closePrices[i] = barSeries.getBar(i).getClosePrice().doubleValue();
            highPrices[i] = barSeries.getBar(i).getHighPrice().doubleValue();
            lowPrices[i] = barSeries.getBar(i).getLowPrice().doubleValue();
            openPrices[i] = barSeries.getBar(i).getOpenPrice().doubleValue();
            volumes[i] = barSeries.getBar(i).getVolume().doubleValue();
        }

        AugmentedDickeyFuller adfClosePrices = new AugmentedDickeyFuller(closePrices);
        return !adfClosePrices.isNeedsDiff();

    }

}
