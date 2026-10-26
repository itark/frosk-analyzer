package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.strategies.rules.CryptoRegimeRule;
import nu.itark.frosk.strategies.rules.MaxBarsHeldRule;
import nu.itark.frosk.strategies.rules.TimeWindowRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.BooleanRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>Strategi 2 — Bitcoin intraday seasonality (21:00–23:00 UTC).</b>
 *
 * <p>Buys at the open of the UTC 21:00 window and holds through 23:00 — the
 * two-hour band when the major equity sessions (NYSE closed 21:00 UTC, Tokyo
 * not yet open, Hong Kong closed, Australia closing) all stand down. Padyšák &
 * Vojtko (Quantpedia 2022/2023) found the 22:00 and 23:00 UTC hourly returns
 * the most economically and statistically significant on Gemini data back to
 * 2015; the updated study reported ~40% annualised with a Calmar near 1.8 and a
 * max drawdown of ~-23% versus BTC's &gt;-70% over the same period. No
 * technical entry filter — the edge is the clock, gated only by a BTC-trend
 * macro filter so it stands aside in a bear market.
 *
 * <h3>Entry (all must be true)</h3>
 * <ul>
 *   <li>The signal bar's end time is in [{@code startHourUtc}, {@code endHourUtc})
 *       — the seasonality window, via {@link TimeWindowRule}.</li>
 *   <li>{@link CryptoRegimeRule} risk-on: BTC above its daily SMA
 *       ({@code crypto.regime.sma.period}). The research specifies a 200-day
 *       filter; this project's canonical BTC-trend gate uses the shared
 *       {@link CryptoRegimeService} SMA instead of adding a second BTC-trend
 *       feature. Fails closed (no entries) when there is no BTC data.</li>
 *   <li>The product is in {@code crypto.timeofday.products} — the research edge
 *       is BTC (optionally ETH) seasonality, not an all-coin effect, so the
 *       strategy stands aside on every other ticker rather than emitting 35
 *       daily paper trades whose edge the research never claimed.</li>
 * </ul>
 *
 * <h3>Exit (first satisfied wins)</h3>
 * <ul>
 *   <li>The bar's end time has left the window — the primary exit, which closes
 *       the position at ~23:00 UTC regardless of when in the window it opened.</li>
 *   <li>Max {@code maxBarsHeld} bars — a safety cap (8 bars = 2h on 15m).</li>
 * </ul>
 *
 * <p>Forward-data strategy: not yet validated on Coinbase/Kraken data — the
 * published backtests are Gemini-only and pre-date the 2024 ETF era. It runs in
 * paper mode to accumulate {@code FeaturedStrategy}/{@code intraday_signal}
 * history for a later pre-registered test; it places no real orders unless live
 * trading is switched on. Gated by {@code crypto.timeofday.enabled}.
 */
@Component
@Slf4j
public class CryptoTimeOfDayIntradayStrategy extends AbstractStrategy
        implements IIndicatorValue, CryptoIntradayStrategy, ISignalStrength {

    private final List<StrategyIndicatorValue> indicatorValues = new java.util.ArrayList<>();

    @Autowired
    private CryptoRegimeService cryptoRegimeService;

    @Autowired
    private SecurityRepository securityRepository;

    @Value("${crypto.timeofday.start.hour.utc:21}")
    private int startHourUtc;

    @Value("${crypto.timeofday.end.hour.utc:23}")
    private int endHourUtc;

    /** Safety cap on hold length (8 = 2h on 15m bars). */
    @Value("${crypto.timeofday.max.bars.held:8}")
    private int maxBarsHeld;

    /** Macro BTC-trend gate on/off — see class javadoc. */
    @Value("${crypto.timeofday.macro.filter.enabled:true}")
    private boolean macroFilterEnabled;

    /** Comma-separated product allowlist — the research universe (BTC, optionally ETH). */
    @Value("${crypto.timeofday.products:BTC-EUR,ETH-EUR}")
    private String productsRaw;

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

        Set<String> products = Arrays.stream(productsRaw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        // ── Entry ─────────────────────────────────────────────────────────
        Rule inWindow = new TimeWindowRule(series, startHourUtc, endHourUtc);
        Rule macroOk = macroFilterEnabled
                ? new CryptoRegimeRule(series, cryptoRegimeService)
                : BooleanRule.TRUE;
        Rule productOk = products.contains(tickerOf(series)) ? BooleanRule.TRUE : BooleanRule.FALSE;

        Rule entryRule = inWindow.and(macroOk).and(productOk);

        // ── Exit ──────────────────────────────────────────────────────────
        // Complement window [end, start) — satisfied once the bar leaves the entry window.
        Rule outsideWindow = new TimeWindowRule(series, endHourUtc, startHourUtc);
        Rule timeExit = new MaxBarsHeldRule(maxBarsHeld);

        Rule exitRule = outsideWindow.or(timeExit);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    /** Resolves the ticker from the series name, which the crypto data layer sets to the security id. */
    private String tickerOf(BarSeries series) {
        try {
            return securityRepository.findById(Long.parseLong(series.getName()))
                    .map(nu.itark.frosk.model.Security::getName)
                    .orElse(series.getName());
        } catch (NumberFormatException e) {
            return series.getName();
        }
    }

    @Override
    public List<StrategyIndicatorValue> getIndicatorValues() {
        return indicatorValues;
    }

    /** Neutralized — the edge is the clock, not a tiered confidence. */
    @Override
    public SignalStrength getSignalStrength(int index) {
        return SignalStrength.BASE;
    }
}
