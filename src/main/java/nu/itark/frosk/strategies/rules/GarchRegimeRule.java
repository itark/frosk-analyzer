package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.regime.Regime;
import nu.itark.frosk.service.RegimeForecastService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate satisfied only when {@link RegimeForecastService#getRegime} at
 * this bar equals {@code requiredRegime}. {@link Regime#UNKNOWN} never
 * matches any required regime, so missing/failed GARCH data fails closed —
 * mirrors {@link CryptoRegimeRule}'s contract.
 *
 * <p>Callers must check {@link RegimeForecastService#isEnabled()} before
 * constructing this rule and substitute a no-op rule when disabled — this
 * rule does not check the flag itself. See
 * {@link nu.itark.frosk.strategies.ShortTermMomentumLongTermStrengthStrategy}
 * for the call-site pattern.
 */
public class GarchRegimeRule extends AbstractRule {
    private final BarSeries barSeries;
    private final RegimeForecastService regimeForecastService;
    private final Regime requiredRegime;

    public GarchRegimeRule(BarSeries barSeries, RegimeForecastService regimeForecastService, Regime requiredRegime) {
        this.barSeries = barSeries;
        this.regimeForecastService = regimeForecastService;
        this.requiredRegime = requiredRegime;
    }

    /** This rule does not use the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return regimeForecastService.getRegime(barSeries, index) == requiredRegime;
    }
}
