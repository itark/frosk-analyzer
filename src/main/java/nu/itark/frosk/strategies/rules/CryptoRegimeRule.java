package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.CryptoRegimeService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate satisfied while the crypto regime is risk-on (BTC above its
 * daily SMA) on the bar's calendar day. Fails closed when no regime data
 * is available.
 */
public class CryptoRegimeRule extends AbstractRule {
    private final BarSeries barSeries;
    private final CryptoRegimeService cryptoRegimeService;
    private final boolean inverted;

    /** Long-side: satisfied while BTC is above its SMA (risk-on). */
    public CryptoRegimeRule(BarSeries barSeries, CryptoRegimeService cryptoRegimeService) {
        this(barSeries, cryptoRegimeService, false);
    }

    /**
     * @param inverted when {@code true} satisfied while BTC is BELOW its SMA (risk-off),
     *                 i.e. the gate for short-side entries.
     */
    public CryptoRegimeRule(BarSeries barSeries, CryptoRegimeService cryptoRegimeService, boolean inverted) {
        this.barSeries = barSeries;
        this.cryptoRegimeService = cryptoRegimeService;
        this.inverted = inverted;
    }

    /** This rule does not use the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean riskOn = cryptoRegimeService.isRiskOn(barSeries.getBar(index).getEndTime());
        return inverted ? !riskOn : riskOn;
    }
}
