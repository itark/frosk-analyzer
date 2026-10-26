package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.IntradayDataService;
import nu.itark.frosk.strategies.indicators.PairSpreadZScoreIndicator;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * <b>Strategi 4 — BTC/ETH stat-arb spread with a rolling OLS hedge ratio
 * (long-the-spread only).</b>
 *
 * <p>Trades the BTC/ETH log-price spread as a mean-reverting process. The hedge
 * ratio β is refit by OLS over a trailing {@code betaWindow} (~5 days on 15m),
 * the spread {@code log(BTC) − β·log(ETH)} is z-scored over the same window
 * ({@link PairSpreadZScoreIndicator}), and the strategy goes long the spread —
 * long BTC — when it is cheap. An IJSRA 2026 study reported Sharpe ~1.58 for
 * cointegration crypto strategies and called the BTC/ETH relationship "very
 * stable across many market regimes"; an MDPI 2022 study found pairs trading
 * stayed positive in 2022 while ETH fell -62%, the bear-market robustness no
 * other candidate here has.
 *
 * <p><b>Long-only first step, by design.</b> The research recommends starting
 * long-only to sidestep the spot-shorting problem on Coinbase: this strategy
 * takes only the long-BTC leg of the spread (the ETH short is implicit, not
 * placed). It therefore trades a <em>directional</em> BTC position conditioned
 * on the spread, not a market-neutral book — the full neutral version waits for
 * Kraken Futures shorting.
 *
 * <p><b>Two-series strategy in a one-series runner.</b> The intraday runner
 * calls {@link #buildStrategy} once per product with that product's series only.
 * This strategy acts on the primary ({@code crypto.statarb.primary.product},
 * BTC-EUR) series alone — for every other product it returns a never-trading
 * strategy — and loads the secondary ({@code ETH-EUR}) series itself from the DB
 * via {@link IntradayDataService}, floor-matching each secondary close onto the
 * primary bar grid by bar end time. Bars with no aligned secondary close get a
 * NaN z-score, where entry simply never fires.
 *
 * <h3>Entry</h3> z-score &lt; {@code entryZ} (spread cheap, default -2.0).
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>z-score &gt; {@code exitZ} — reverted (default -0.5).</li>
 *   <li>z-score &lt; {@code stopZ} — divergence stop (default -4.0).</li>
 *   <li>Max {@code maxBarsHeld} bars.</li>
 * </ul>
 *
 * <p>Forward-data strategy — the cited results are not on this project's
 * Coinbase/Kraken data, and cointegration stability (incl. the Nov-2022 FTX
 * break) is unconfirmed here. Runs in paper mode; places no real orders unless
 * live trading is on. Gated by {@code crypto.statarb.enabled}.
 */
@Component
@Slf4j
public class CryptoBtcEthStatArbIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    @Autowired(required = false)
    private IntradayDataService cryptoIntradayDataService;

    @Autowired
    private SecurityRepository securityRepository;

    @Value("${crypto.statarb.primary.product:BTC-EUR}")
    private String primaryProduct;

    @Value("${crypto.statarb.secondary.product:ETH-EUR}")
    private String secondaryProduct;

    /** Rolling OLS / z-score window (500 = ~5 days on 15m bars). */
    @Value("${crypto.statarb.window:500}")
    private int window;

    @Value("${crypto.statarb.entry.z:-2.0}")
    private double entryZ;

    @Value("${crypto.statarb.exit.z:-0.5}")
    private double exitZ;

    @Value("${crypto.statarb.stop.z:-4.0}")
    private double stopZ;

    @Value("${crypto.statarb.max.bars.held:96}")
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

        // Acts only on the primary (BTC) series; everything else never trades.
        if (!primaryProduct.equals(tickerOf(series)) || cryptoIntradayDataService == null) {
            return noTradeStrategy();
        }

        double[] secondaryAligned = buildAlignedSecondaryClose(series);
        if (secondaryAligned == null) {
            return noTradeStrategy();
        }

        PairSpreadZScoreIndicator z = new PairSpreadZScoreIndicator(series, secondaryAligned, window);
        setIndicatorValues(z, "spreadZ");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule cheap = new UnderIndicatorRule(z, DoubleNum.valueOf(entryZ));

        // ── Exit ──────────────────────────────────────────────────────────
        Rule reverted = new OverIndicatorRule(z, DoubleNum.valueOf(exitZ));
        Rule diverged = new UnderIndicatorRule(z, DoubleNum.valueOf(stopZ));
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        return new BaseStrategy(this.getClass().getSimpleName(), cheap, reverted.or(diverged).or(timeExit));
    }

    /**
     * Floor-matches the secondary product's closes onto the primary bar grid by
     * bar end time. Returns {@code null} when the secondary series is unavailable
     * (then the strategy never trades); individual bars with no aligned close get
     * {@code NaN}.
     */
    private double[] buildAlignedSecondaryClose(BarSeries primary) {
        Map<Security, BarSeries> all = cryptoIntradayDataService.buildAllSeriesFromDb();
        BarSeries secondary = all.entrySet().stream()
                .filter(e -> secondaryProduct.equals(e.getKey().getName()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        if (secondary == null || secondary.getBarCount() == 0) {
            log.warn("CryptoBtcEthStatArb: secondary series '{}' unavailable — no trades", secondaryProduct);
            return null;
        }

        NavigableMap<Long, Double> byEpoch = new TreeMap<>();
        for (int i = secondary.getBeginIndex(); i <= secondary.getEndIndex(); i++) {
            byEpoch.put(secondary.getBar(i).getEndTime().toEpochSecond(),
                    secondary.getBar(i).getClosePrice().doubleValue());
        }

        double[] aligned = new double[primary.getEndIndex() + 1];
        Arrays.fill(aligned, Double.NaN);
        for (int i = primary.getBeginIndex(); i <= primary.getEndIndex(); i++) {
            Map.Entry<Long, Double> e = byEpoch.floorEntry(primary.getBar(i).getEndTime().toEpochSecond());
            if (e != null) {
                aligned[i] = e.getValue();
            }
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
