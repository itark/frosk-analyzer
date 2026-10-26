package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.rules.CountSatisfiedRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.RecentlySatisfiedRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * <b>Brainstorm #6 (step 6b) — ensemble / majority-vote meta-strategy.</b>
 *
 * <p>Takes a long when at least {@code voteThreshold} of three constituent
 * strategies have entered <em>recently</em> — within the trailing {@code
 * recencyWindowBars} bars, via {@link RecentlySatisfiedRule} — and exits as
 * soon as any constituent's exit rule fires (or on a safety stop / time-out).
 * The thesis (research §5 #6): a vote between genuinely decorrelated signals
 * trades less but at higher confidence. Step 6a ({@code
 * CryptoSignalCorrelationIT}) measured the three constituents' pairwise
 * correlation ≈ 0 (below the 0.4 gate) — weak evidence on a 30-day sample, so
 * this runs in paper mode and its go/no-go is revisited on fuller data.
 *
 * <p><b>Why a recency window, not a same-bar vote.</b> The first cut voted on
 * all three entry rules firing on the SAME bar and never traded (0 trades,
 * 10 coins/30d): entry triggers are sparse, so exact-bar co-occurrence is ≈ 0,
 * and VWAP (21–23 UTC) and Night (01–07 UTC) occupy disjoint windows that can
 * never coincide. Counting "entered within the last {@code recencyWindowBars}"
 * instead lets genuine near-simultaneous agreement register — and a large
 * enough window can even bridge the disjoint VWAP/Night windows — without
 * collapsing to a trivial union the way {@code voteThreshold = 1} would.
 *
 * <p>Constituents (the trio chosen 2026-10-26): {@link
 * CryptoVWAPReversionIntradayStrategy}, {@link
 * CryptoLiquiditySweepIntradayStrategy}, {@link
 * CryptoNightReversionIntradayStrategy}. It reuses their FULL configured entry/
 * exit rules — the same outputs 6a measured — rather than re-deriving signals,
 * by calling each one's {@code buildStrategy(series)} and reading its
 * {@link Strategy#getEntryRule()} / {@link Strategy#getExitRule()}. Each
 * constituent rule is evaluated against the ENSEMBLE's own {@code TradingRecord},
 * so their position-aware stop/time/cooldown legs reckon from the ensemble's
 * entry.
 *
 * <p>Conservative-by-design filter (few, high-agreement trades);
 * {@code crypto.ensemble.vote.threshold} and {@code crypto.ensemble.recency.bars}
 * are exposed to experiment. Larger recency bridges the disjoint VWAP/Night
 * windows but loosens what "agreement" means — tune against forward data, not
 * the 30-day in-sample.
 *
 * <p>Forward-data strategy — gated by {@code crypto.ensemble.enabled}; places no
 * real orders unless live trading is on.
 */
@Component
@Slf4j
public class CryptoEnsembleIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    @Autowired
    private CryptoVWAPReversionIntradayStrategy vwap;

    @Autowired
    private CryptoLiquiditySweepIntradayStrategy liquiditySweep;

    @Autowired
    private CryptoNightReversionIntradayStrategy nightReversion;

    @Value("${crypto.ensemble.vote.threshold:2}")
    private int voteThreshold;

    /** Recency window: a constituent counts as "voting" if it entered within this many bars (8 = 2h on 15m). */
    @Value("${crypto.ensemble.recency.bars:8}")
    private int recencyBars;

    /** Exit vote: exit once this many constituents' exit rules fire (default 1 = any). */
    @Value("${crypto.ensemble.exit.vote.threshold:1}")
    private int exitVoteThreshold;

    /** Safety stop below entry in percent. */
    @Value("${crypto.ensemble.stop.pct:2.7}")
    private double stopPct;

    /** Safety time exit (48 = 12h on 15m). */
    @Value("${crypto.ensemble.max.bars.held:48}")
    private int maxBarsHeld;

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

        // Reuse each constituent's full configured entry/exit rules — the same
        // outputs measured in 6a. buildStrategy() fully rebuilds each constituent
        // for THIS series (the codebase's normal per-run rebuild), so reading its
        // rules here has no cross-run state.
        Strategy vwapStrat = vwap.buildStrategy(series);
        Strategy sweepStrat = liquiditySweep.buildStrategy(series);
        Strategy nightStrat = nightReversion.buildStrategy(series);

        // ── Entry: >= voteThreshold of the three entered within recencyBars ─
        Rule entryVote = new CountSatisfiedRule(voteThreshold,
                new RecentlySatisfiedRule(vwapStrat.getEntryRule(), recencyBars),
                new RecentlySatisfiedRule(sweepStrat.getEntryRule(), recencyBars),
                new RecentlySatisfiedRule(nightStrat.getEntryRule(), recencyBars));

        // ── Exit: >= exitVoteThreshold of the exit rules fire (default any), ─
        //         or a safety stop / time-out ──────────────────────────────
        Rule exitVote = new CountSatisfiedRule(exitVoteThreshold,
                vwapStrat.getExitRule(), sweepStrat.getExitRule(), nightStrat.getExitRule());
        Rule stopLoss = new StopLossRule(close, stopPct);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = exitVote.or(stopLoss).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryVote, exitRule);
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
