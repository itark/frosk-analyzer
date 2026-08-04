package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.indicators.PreviousDayExtremeIndicator;
import nu.itark.frosk.strategies.rules.LiquiditySweepEntryRule;
import nu.itark.frosk.strategies.rules.LiquiditySweepExitRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.ZoneId;
import java.util.List;

/**
 * PDH/PDL liquidity sweep — 15m Coinbase bars, anchored to the UTC day.
 *
 * <p>Price that penetrates the previous UTC day's low and closes back above it has
 * swept resting stop-loss liquidity; the trade is the reversion toward the previous
 * day's high, stopped below the sweep extreme.
 *
 * <h3>This class implements a pre-registered specification.</h3>
 * {@code ~/itark/PREREG_liquidity_sweep_15m.md}, written 2026-08-04 BEFORE the data
 * needed to evaluate it existed. Sections §3-§5 are transcribed here verbatim.
 * <b>Do not change any parameter, add any filter, or alter the entry/exit geometry
 * without recording it in the deviation log in §10 of that document.</b> A tuned
 * version is a different, unregistered hypothesis and its results cannot be trusted —
 * that is precisely how CANSLIM came to show +2.3%/trade in this codebase.
 *
 * <h3>Why this idea was worth coding at all</h3>
 * Every other strategy here died on cost. At a 6.46% median daily crypto range, a
 * target a full range away puts the 0.32% all-in round trip at roughly 7% of the
 * target, against 27% for VWAPReversion and 300%+ for the equity library. That is a
 * necessary condition for an edge, not evidence of one.
 *
 * <h3>Long only</h3>
 * The mirror setup (sweep PDH -> short) is not implementable: Coinbase spot cannot
 * be shorted. It is excluded rather than assumed symmetric.
 */
@Slf4j
@Component
public class CryptoLiquiditySweepIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final ZoneId UTC = ZoneId.of("UTC");

    /** Bars after the sweep within which the reclaim must close. §4.2 = 4 bars (1h). */
    @Value("${crypto.sweep.confirm.bars:4}")
    private int confirmBars;

    /** Minimum previous-day range as % of PDL. §3 = 2.0. */
    @Value("${crypto.sweep.min.range.pct:2.0}")
    private double minRangePct;

    /** Time stop in bars. §5 = 96 (24h on 15m bars). */
    @Value("${crypto.sweep.max.bars.held:96}")
    private int maxBarsHeld;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        PreviousDayExtremeIndicator pdh = new PreviousDayExtremeIndicator(series, UTC, true);
        PreviousDayExtremeIndicator pdl = new PreviousDayExtremeIndicator(series, UTC, false);

        setIndicatorValues(new ClosePriceIndicator(series), "close");
        setIndicatorValues(pdh, "pdh");
        setIndicatorValues(pdl, "pdl");

        Rule entryRule = new LiquiditySweepEntryRule(series, pdh, pdl, UTC, confirmBars, minRangePct);
        Rule exitRule = new LiquiditySweepExitRule(series, pdh, pdl, confirmBars, maxBarsHeld);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }
}
