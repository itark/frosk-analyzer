package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.CryptoMarketRegime;
import nu.itark.frosk.service.CryptoRegimeService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate satisfied while {@link CryptoRegimeService#getRegime} equals the
 * required three-state regime (RANGING / TRENDING_UP / TRENDING_DOWN) on the
 * bar's calendar day.
 *
 * <p>Stricter than the older binary {@link CryptoRegimeRule}: {@code
 * TRENDING_UP} requires BTC above its SMA <em>and</em> ADX at/above the trend
 * threshold, so it is a subset of "risk-on" — a market can be above its SMA
 * while ADX has collapsed (RANGING), which the binary gate alone cannot see.
 * Confirmed in production data: 2026-07-22 to 2026-07-30, BTC stayed above its
 * 50-day SMA the whole week (binary gate would have allowed every entry) while
 * ADX fell from ~26 to ~18 (RANGING) — {@code CryptoEMACrossLongIntradayStrategy}
 * and {@code CryptoRangeBreakoutIntradayStrategy} lost -603% and -232% over
 * that week on 16-22% win rates, while the regime-agnostic
 * {@code CryptoVWAPReversionIntradayStrategy} made +30% in the same market.
 */
public class CryptoMarketRegimeRule extends AbstractRule {

    private final BarSeries barSeries;
    private final CryptoRegimeService cryptoRegimeService;
    private final CryptoMarketRegime required;

    public CryptoMarketRegimeRule(BarSeries barSeries, CryptoRegimeService cryptoRegimeService,
                                  CryptoMarketRegime required) {
        this.barSeries = barSeries;
        this.cryptoRegimeService = cryptoRegimeService;
        this.required = required;
    }

    /** This rule does not use the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        CryptoMarketRegime regime = cryptoRegimeService.getRegime(barSeries.getBar(index).getEndTime());
        return regime == required;
    }
}
