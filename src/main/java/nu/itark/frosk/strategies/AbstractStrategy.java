package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.service.BarSeriesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.surus.math.AugmentedDickeyFuller;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
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

    @Value("${frosk.inherent.exitrule}")
    public boolean inherentExitRule = true;

    public Boolean stationary;

    AbstractStrategy(BarSeries barSeries) {
        this.barSeries = barSeries;
       // this.barSeriesWithForecast = barSeriesService.withArimaForecast(barSeries, 3);
       // this.stationary = isStationary();
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

      //  log.info("adfClosePrices.isNeedsDiff()="+adfClosePrices.isNeedsDiff());

        return adfClosePrices.isNeedsDiff();

    }

}
