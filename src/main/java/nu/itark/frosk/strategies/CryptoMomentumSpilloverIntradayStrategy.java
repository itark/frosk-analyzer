package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.IntradayDataService;
import nu.itark.frosk.strategies.indicators.DoubleArrayIndicator;
import nu.itark.frosk.strategies.indicators.PercentChangeIndicator;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>Brainstorm #4 — cross-coin momentum decay / spillover (Coinbase spot,
 * long).</b>
 *
 * <p>The research thesis ("spillover effects" in crypto): when a major (BTC or
 * ETH) breaks out strongly on high volume, altcoins that have NOT moved yet
 * tend to follow within a few bars. So: on a bar where a master broke out UP
 * (&gt; {@code breakoutPct} on 15m with volume &gt; {@code volumeMult}× its
 * rolling average), go long any lagging target (one whose own move over the
 * same window is still small), and exit after {@code holdBars} candles — the
 * decay is fast, so the edge is a short follow-through, not a trend ride.
 * Long-only on Coinbase spot, so only UP breakouts are acted on.
 *
 * <p><b>Cross-series, like {@link CryptoBtcEthStatArbIntradayStrategy}.</b> The
 * runner calls {@link #buildStrategy} per product with that product's series
 * only; this strategy loads the master series itself from the DB via {@link
 * IntradayDataService}, computes each master's breakout-up flag per bar, merges
 * them (OR), and floor-aligns the flag onto the target bar grid by bar end time
 * (also accepting a breakout in the previous {@code breakoutWindowBars} bars, so
 * a laggard still qualifies a bar or two late). Masters are never traded as
 * their own laggards — the strategy returns a never-trading strategy for a
 * master ticker.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>A master broke out UP on this bar (or within {@code breakoutWindowBars}).</li>
 *   <li>The target's own |% change| over {@code lagLookback} bars &lt;
 *       {@code lagMaxPct} — it is lagging, not already moving.</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>Max {@code holdBars} bars (the 2–4 candle decay window).</li>
 *   <li>{@code stopPct} below entry — catastrophic stop.</li>
 * </ul>
 *
 * <p>Forward-data strategy — not yet validated on this project's data. Runs in
 * paper mode; places no real orders unless live trading is on. Gated by
 * {@code crypto.spillover.enabled}. Overfit guard: keep {@code holdBars} and the
 * lag window short and few-parameter.
 */
@Component
@Slf4j
public class CryptoMomentumSpilloverIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final long BAR_SECONDS = 900; // 15 minutes

    @Autowired(required = false)
    private IntradayDataService cryptoIntradayDataService;

    @Autowired
    private SecurityRepository securityRepository;

    @Value("${crypto.spillover.master.products:BTC-EUR,ETH-EUR}")
    private String masterProductsRaw;

    /** Master 15m return (percent) above which it counts as a breakout. */
    @Value("${crypto.spillover.breakout.pct:1.5}")
    private double breakoutPct;

    /** Rolling window for the master's average volume. */
    @Value("${crypto.spillover.volume.lookback:20}")
    private int volumeLookback;

    /** Master bar volume must exceed this multiple of its average to count as high-volume. */
    @Value("${crypto.spillover.volume.mult:1.5}")
    private double volumeMult;

    /** A master breakout in the current or previous N bars still qualifies a laggard. */
    @Value("${crypto.spillover.breakout.window.bars:2}")
    private int breakoutWindowBars;

    /** Bars over which the target's own move is measured for the lag test. */
    @Value("${crypto.spillover.lag.lookback:1}")
    private int lagLookback;

    /** Target counts as lagging while its |% change| over lagLookback stays below this. */
    @Value("${crypto.spillover.lag.max.pct:0.5}")
    private double lagMaxPct;

    /** Hold length — the 2–4 candle decay window. */
    @Value("${crypto.spillover.hold.bars:3}")
    private int holdBars;

    @Value("${crypto.spillover.stop.pct:2.0}")
    private double stopPct;

    private ClosePriceIndicator close;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        setIndicatorValues(close, "close");

        Set<String> masters = Arrays.stream(masterProductsRaw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        // Never trade a master as its own laggard; nothing to do without the data service.
        if (cryptoIntradayDataService == null || masters.contains(tickerOf(series))) {
            return noTradeStrategy();
        }

        double[] masterBreakout = buildAlignedMasterBreakout(series, masters);
        if (masterBreakout == null) {
            return noTradeStrategy();
        }

        DoubleArrayIndicator masterBrokeUp = new DoubleArrayIndicator(series, masterBreakout);
        PercentChangeIndicator targetChange = new PercentChangeIndicator(close, lagLookback);
        TransformIndicator targetAbsChange = TransformIndicator.abs(targetChange);

        setIndicatorValues(masterBrokeUp, "masterBrokeUp");
        setIndicatorValues(targetAbsChange, "targetAbsChangePct");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule masterBreakoutUp = new OverIndicatorRule(masterBrokeUp, DoubleNum.valueOf(0.5));
        Rule targetLagging = new UnderIndicatorRule(targetAbsChange, DoubleNum.valueOf(lagMaxPct));

        Rule entryRule = masterBreakoutUp.and(targetLagging);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule timeExit = new MaxBarsHeldRule(holdBars);
        Rule stopLoss = new StopLossRule(close, stopPct);

        Rule exitRule = timeExit.or(stopLoss);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /**
     * Builds the per-target-bar master-breakout flag (1.0 = a master broke out
     * up at this bar or within {@code breakoutWindowBars} before it, else 0.0).
     * Returns {@code null} if no master series is available.
     */
    private double[] buildAlignedMasterBreakout(BarSeries target, Set<String> masters) {
        Map<Security, BarSeries> all = cryptoIntradayDataService.buildAllSeriesFromDb();
        Set<Long> breakoutEpochs = new HashSet<>();
        boolean anyMaster = false;

        for (Map.Entry<Security, BarSeries> e : all.entrySet()) {
            if (!masters.contains(e.getKey().getName())) {
                continue;
            }
            anyMaster = true;
            BarSeries ms = e.getValue();
            if (ms.getBarCount() == 0) {
                continue;
            }
            ClosePriceIndicator mClose = new ClosePriceIndicator(ms);
            VolumeIndicator mVol = new VolumeIndicator(ms);
            SMAIndicator mAvgVol = new SMAIndicator(mVol, volumeLookback);
            for (int j = ms.getBeginIndex() + 1; j <= ms.getEndIndex(); j++) {
                double prev = mClose.getValue(j - 1).doubleValue();
                if (prev <= 0) {
                    continue;
                }
                double retPct = (mClose.getValue(j).doubleValue() / prev - 1.0) * 100.0;
                double vol = mVol.getValue(j).doubleValue();
                double avg = mAvgVol.getValue(j).doubleValue();
                if (retPct > breakoutPct && avg > 0 && vol > volumeMult * avg) {
                    breakoutEpochs.add(ms.getBar(j).getEndTime().toEpochSecond());
                }
            }
        }
        if (!anyMaster) {
            log.warn("CryptoMomentumSpillover: no master series {} available — no trades", masters);
            return null;
        }

        double[] aligned = new double[target.getEndIndex() + 1];
        for (int i = target.getBeginIndex(); i <= target.getEndIndex(); i++) {
            long e = target.getBar(i).getEndTime().toEpochSecond();
            boolean broke = false;
            for (int k = 0; k < breakoutWindowBars; k++) {
                if (breakoutEpochs.contains(e - k * BAR_SECONDS)) {
                    broke = true;
                    break;
                }
            }
            aligned[i] = broke ? 1.0 : 0.0;
        }
        return aligned;
    }

    private Strategy noTradeStrategy() {
        return new BaseStrategy(this.getClass().getSimpleName(), BooleanRule.FALSE, BooleanRule.TRUE);
    }

    private String tickerOf(BarSeries series) {
        try {
            return securityRepository.findById(Long.parseLong(series.getName()))
                    .map(Security::getName)
                    .orElse(series.getName());
        } catch (NumberFormatException e) {
            return series.getName();
        }
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /** Neutralized — no un-backtested sizing, consistent with the other crypto strategies. */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }
}
