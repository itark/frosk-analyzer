package nu.itark.frosk.strategies.rules;

import nu.itark.frosk.service.LstmSignalFilterService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Entry gate satisfied only when {@link LstmSignalFilterService#isBuySignal}
 * is true at this bar. Unlike {@link GarchRegimeRule} (which takes a required
 * {@code Regime} — GARCH+ADX has more than two outcomes), the LSTM signal is
 * inherently binary, so this rule takes no extra parameter: it just asks the
 * service. Fails closed on any missing/failed data — mirrors {@link
 * CryptoRegimeRule} and {@link GarchRegimeRule}'s contract.
 *
 * <p>Callers must check {@link LstmSignalFilterService#isEnabled()} before
 * constructing this rule and substitute a no-op rule when disabled — this
 * rule does not check the flag itself. See
 * {@link nu.itark.frosk.strategies.CryptoEMACrossLongIntradayStrategy} for
 * the call-site pattern.
 */
public class LstmSignalRule extends AbstractRule {
    private final BarSeries barSeries;
    private final LstmSignalFilterService lstmSignalFilterService;

    public LstmSignalRule(BarSeries barSeries, LstmSignalFilterService lstmSignalFilterService) {
        this.barSeries = barSeries;
        this.lstmSignalFilterService = lstmSignalFilterService;
    }

    /** This rule does not use the {@code tradingRecord}. */
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return lstmSignalFilterService.isBuySignal(barSeries, index);
    }
}
