package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.analysis.Returns;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class BetaIndicator extends CachedIndicator<Num> {
    private final Indicator<Num> stockReturnIndicator;
    private final Indicator<Num> marketReturnIndicator;
    private final int lookbackPeriod;

    public BetaIndicator(Indicator<Num> stockClose,
                         Indicator<Num> marketClose,
                         int lookbackPeriod) {
        super(stockClose);
        this.lookbackPeriod = lookbackPeriod;

        this.stockReturnIndicator = new LogReturnIndicator(stockClose);
        this.marketReturnIndicator = new LogReturnIndicator(marketClose);
    }

    @Override
    protected Num calculate(int index) {
        if (index < lookbackPeriod) {
            return numOf(0);
        }

        Num meanStock = average(stockReturnIndicator, index);
        Num meanMarket = average(marketReturnIndicator, index);

        Num covariance = numOf(0);
        Num variance = numOf(0);

        for (int i = index - lookbackPeriod + 1; i <= index; i++) {
            Num stockDev = stockReturnIndicator.getValue(i).minus(meanStock);
            Num marketDev = marketReturnIndicator.getValue(i).minus(meanMarket);

            covariance = covariance.plus(stockDev.multipliedBy(marketDev));
            variance = variance.plus(marketDev.multipliedBy(marketDev));
        }

        if (variance.isZero()) {
            return numOf(0);
        }



        if (covariance.dividedBy(variance).isGreaterThan(numOf(1.0))) {
            log.info("Beta > 1.0:{}",covariance.dividedBy(variance));
        } else {
            log.info("Beta < 1.0:{}",covariance.dividedBy(variance));
        }



        return covariance.dividedBy(variance);
    }

    private Num average(Indicator<Num> indicator, int index) {
        Num sum = numOf(0);
        for (int i = index - lookbackPeriod + 1; i <= index; i++) {
            sum = sum.plus(indicator.getValue(i));
        }
        return sum.dividedBy(numOf(lookbackPeriod));
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}

