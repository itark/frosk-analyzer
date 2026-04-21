package nu.itark.frosk.strategies.hedge;

import lombok.RequiredArgsConstructor;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.IIndicatorValue;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Risk-off when the US yield curve is deeply inverted:
 * 10Y Treasury yield (^TNX) minus 13-week T-bill yield (^IRX) < −0.5 (−50 bps).
 *
 * A deep inversion historically precedes global recessions by 6–18 months and
 * is a reliable leading indicator even for non-US equity markets including OMXS30.
 *
 * Both ^TNX and ^IRX report yield as percent (e.g. 4.5 = 4.5%), so
 * spread = TNX − IRX in percentage-point terms. Threshold −0.5 = −50 bps.
 */
@Component
@RequiredArgsConstructor
public class YieldCurveSpreadStrategy implements IIndicatorValue {

    private final BarSeriesService barSeriesService;

    public Strategy buildStrategy() {
        BarSeries tnxSeries = barSeriesService.getDataSet("^TNX", false, false);
        BarSeries irxSeries = barSeriesService.getDataSet("^IRX", false, false);

        List<BarSeries> aligned = BarSeriesAligner.alignAndTruncate(
                List.of(tnxSeries, irxSeries), tnxSeries.getBarCount());
        BarSeries alignedTNX = aligned.get(0);
        BarSeries alignedIRX = aligned.get(1);

        ClosePriceIndicator tnxClose = new ClosePriceIndicator(alignedTNX);
        ClosePriceIndicator irxClose = new ClosePriceIndicator(alignedIRX);

        // spread = 10Y yield − 13W yield (in percentage points)
        CachedIndicator<Num> spread = new CachedIndicator<>(alignedTNX) {
            @Override
            protected Num calculate(int index) {
                return tnxClose.getValue(index).minus(irxClose.getValue(index));
            }
            @Override
            public int getUnstableBars() { return 0; }
        };

        // Risk-off (SELL): spread < −0.5 (deep inversion)
        Rule exitRule = new UnderIndicatorRule(spread, DoubleNum.valueOf(-0.5));
        // Risk-on (BUY): spread not deeply inverted
        Rule entryRule = exitRule.negation();

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
