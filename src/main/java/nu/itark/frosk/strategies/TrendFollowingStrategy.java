package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * Time-series momentum (trend following) on futures.
 *
 * <p>Long while the trailing {@code lookbackBars}-day return is positive, flat
 * otherwise. This is the Moskowitz-Ooi-Pedersen 12-month rule, restricted to the
 * long-or-flat form because shorting is not available to this account.
 *
 * <h3>Measured performance — read this before trusting it</h3>
 * Tested on 32 futures markets, 2001-2026, 296 monthly observations, inverse-vol
 * sized, 0.10% round-trip cost, per {@code ~/itark/PREREG_tsmom_futures.md}:
 *
 * <pre>
 *   variant           per month      t     annual   Sharpe   maxDD
 *   long/short          +0.115%    1.64     +1.4%    0.33   -11.6%
 *   long-or-flat (this) +0.230%    1.99     +2.8%    0.44   -19.4%
 *   always long         +0.243%    2.41     +2.9%    0.58   -17.9%
 * </pre>
 *
 * <p><b>The pre-registered test FAILED.</b> The long/short specification cleared two of
 * three criteria — it beat a random-sign null at t=2.80, which no other strategy in this
 * project has managed, so the signal does carry information — but Sharpe 0.33 missed the
 * 0.40 bar. This long-or-flat variant reaches 0.44, yet still loses to simply holding the
 * basket on BOTH return and drawdown. Trend following is supposed to cut drawdowns; over
 * this period and this universe it deepened them.
 *
 * <p>Implemented at the owner's explicit direction after that evidence was presented.
 * It is not a validated edge. Two caveats recorded in §9 of the pre-registration cut in
 * its favour and should not be forgotten: 2000-2026 excludes the 1970s-80s era that
 * flatters long histories, and 32 markets is half the 67 the literature diversifies
 * across — the split-half shows 2001-2014 at Sharpe 0.41 versus 2014-2026 at 0.22.
 *
 * <p>Portfolio construction (inverse-volatility sizing across markets, monthly rebalance)
 * is NOT here. This class answers only "is this market in an uptrend"; sizing belongs to
 * the portfolio layer, and equal-weighting these signals instead would concentrate the
 * book in natural gas, whose daily vol is ~40x that of the 2-year note.
 */
@Slf4j
@Component
public class TrendFollowingStrategy extends AbstractStrategy implements IIndicatorValue {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    /** Lookback in trading days. 252 = the canonical 12-month rule; do not tune. */
    @Value("${frosk.trend.lookback.bars:252}")
    private int lookbackBars;

    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        // Price N bars ago. Comparing close to it IS the sign of the N-day return,
        // without needing a ratio indicator: close > close[-N] <=> return > 0.
        PreviousValueIndicator past = new PreviousValueIndicator(close, lookbackBars);

        setIndicatorValues(close, "close");
        setIndicatorValues(past, "close" + lookbackBars + "dAgo");

        Rule entryRule = new OverIndicatorRule(close, past);
        Rule exitRule = new UnderIndicatorRule(close, past);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
