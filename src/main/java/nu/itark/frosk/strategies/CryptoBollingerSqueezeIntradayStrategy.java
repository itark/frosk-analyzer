package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.strategies.indicators.BollingerBandWidthPercentileIndicator;
import nu.itark.frosk.strategies.rules.AtrStopLossRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

/**
 * <b>Strategi 3 — Bollinger Band squeeze breakout with volume confirmation
 * (LONG side).</b>
 *
 * <p>Fundamentally different from the mean-reversion book: it trades
 * <em>volatility expansion</em> after compression, not a stretch inside a
 * trend. A "squeeze" is a Bollinger band-width reading below its {@code
 * squeezePct}-th percentile over the last {@code bbwLookback} bars (compressed
 * volatility); the entry fires when a bar then closes above the upper band
 * <em>and</em> its volume beats the rolling {@code volumeSmaPeriod}-bar average,
 * i.e. a genuine expansion rather than a drift through the band. CoinQuant
 * reported +94.8% / Sharpe 1.95 on an ETH 4h BB-breakout (small sample, 21
 * trades); a volatility filter is repeatedly found to lift Sharpe by removing
 * low-quality setups. Win rate below 50% is expected — a few large winners carry
 * breakout systems.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>{@link BollingerBandWidthPercentileIndicator} &lt; {@code squeezePct} —
 *       the band is in a squeeze.</li>
 *   <li>Close above the upper band — the breakout.</li>
 *   <li>Volume &gt; SMA(volume, {@code volumeSmaPeriod}) — expansion confirmed.</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>Close crosses back below the middle band — the move has failed.</li>
 *   <li>{@link AtrStopLossRule}: {@code atrStopMult}× ATR below entry.</li>
 *   <li>Max {@code maxBarsHeld} bars (8 = 2h on 15m).</li>
 * </ul>
 *
 * <p>Forward-data strategy — not yet validated on this project's data (the cited
 * backtests are 4h and small-sample). Runs in paper mode to accumulate history
 * for a later pre-registered test; places no real orders unless live trading is
 * on. Gated by {@code crypto.bbsqueeze.enabled}.
 */
@Component
@Slf4j
public class CryptoBollingerSqueezeIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    private static final int ATR_PERIOD = 14;

    @Value("${crypto.bbsqueeze.ma.period:20}")
    private int maPeriod;

    @Value("${crypto.bbsqueeze.k:2.0}")
    private double k;

    @Value("${crypto.bbsqueeze.bbw.lookback:100}")
    private int bbwLookback;

    /** Band-width percentile below which the band counts as squeezed. */
    @Value("${crypto.bbsqueeze.squeeze.pct:10.0}")
    private double squeezePct;

    @Value("${crypto.bbsqueeze.volume.sma.period:20}")
    private int volumeSmaPeriod;

    @Value("${crypto.bbsqueeze.atr.stop.mult:1.0}")
    private double atrStopMult;

    @Value("${crypto.bbsqueeze.max.bars.held:8}")
    private int maxBarsHeld;

    private ClosePriceIndicator close;
    private BollingerBandsMiddleIndicator middle;
    private BollingerBandsUpperIndicator upper;
    private BollingerBandWidthPercentileIndicator bbwPct;

    @Override
    public Strategy buildStrategy(BarSeries series) {
        super.setInherentExitRule();
        indicatorValues.clear();
        if (series == null) {
            throw new IllegalArgumentException("BarSeries cannot be null");
        }
        super.barSeries = series;

        close = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(close, maPeriod);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(close, maPeriod);
        middle = new BollingerBandsMiddleIndicator(sma);
        upper = new BollingerBandsUpperIndicator(middle, sd, DoubleNum.valueOf(k));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, sd, DoubleNum.valueOf(k));
        bbwPct = new BollingerBandWidthPercentileIndicator(series, maPeriod, k, bbwLookback);

        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volumeSma = new SMAIndicator(volume, volumeSmaPeriod);

        setIndicatorValues(close, "close");
        setIndicatorValues(middle, "bbMiddle");
        setIndicatorValues(upper, "bbUpper");
        setIndicatorValues(lower, "bbLower");
        setIndicatorValues(bbwPct, "bbwPct");

        // ── Entry ─────────────────────────────────────────────────────────
        Rule squeeze = new UnderIndicatorRule(bbwPct, DoubleNum.valueOf(squeezePct));
        Rule breakout = new OverIndicatorRule(close, upper);
        Rule volumeConfirm = new OverIndicatorRule(volume, volumeSma);

        Rule entryRule = squeeze.and(breakout).and(volumeConfirm);

        // ── Exit ──────────────────────────────────────────────────────────
        Rule backToMiddle = new CrossedDownIndicatorRule(close, middle);
        Rule atrStop = new AtrStopLossRule(series, ATR_PERIOD, atrStopMult);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = backToMiddle.or(atrStop).or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
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
