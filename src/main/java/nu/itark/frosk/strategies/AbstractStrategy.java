package nu.itark.frosk.strategies;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.RecommendationTrendRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.indicators.MedianTurnoverIndicator;
import nu.itark.frosk.strategies.rules.LiquidityRule;
import nu.itark.frosk.strategies.rules.StopLossRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.TrailingStopLossRule;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public abstract  class AbstractStrategy {
    private Rule exitRule;
    protected  BarSeries barSeries;
    BarSeries barSeriesWithForecast;
    public Boolean inherentExitRule;

    @Value("${frosk.strategy.catastrophic.stop.pct:15.0}")
    protected double catastrophicStopPct;

    /** Largest share of median daily turnover one position may take, in percent. 0 disables. */
    @Value("${frosk.liquidity.max.position.pct.of.turnover:1.0}")
    private double maxPositionPctOfTurnover;

    /** Bars used for the median turnover estimate (~1 quarter of trading days). */
    @Value("${frosk.liquidity.turnover.bars:60}")
    private int liquidityTurnoverBars;

    @Autowired
    private TradingAccountService tradingAccountService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private RecommendationTrendRepository recommendationTrendRepository;

    void setInherentExitRule() {
        inherentExitRule= tradingAccountService.getActiveTradingAccount().getAccountType().getInherentExitRule();
    }

    protected Rule catastrophicStopRule() {
        ClosePriceIndicator close = new ClosePriceIndicator(barSeries);
        return new StopLossRule(close, catastrophicStopPct);
    }

    /**
     * Entry gate: only allow trades the intended position size could realistically
     * be filled at, measured as a share of the instrument's median daily turnover.
     *
     * <p>Backtests fill any size at the bar close, which invents liquidity that does
     * not exist. Measured on this universe, a 20,000 SEK position exceeds 1% of median
     * daily turnover in a large share of the securities these strategies trade — those
     * fills are fiction, and the "profit" attached to them is noise in both directions.
     *
     * <p>Fails OPEN (always-true) when disabled by configuration or when no position
     * value is available: a missing setting must never silently suppress every signal.
     * The underlying {@link MedianTurnoverIndicator} fails CLOSED on missing data, so
     * an instrument with no volume history is still blocked.
     *
     * <p>Do NOT register the turnover indicator via {@code setIndicatorValues()} —
     * {@code strat_indicator_value.value_} is NUMERIC(12,6) and overflows on turnover
     * in the millions, which also poisons the Hibernate session for the whole run.
     */
    protected Rule liquidityRule(BarSeries series) {
        if (maxPositionPctOfTurnover <= 0) {
            return new BooleanRule(true);
        }
        BigDecimal positionValue = null;
        try {
            positionValue = tradingAccountService.getDefaultActiveTradingAccount().getPositionValue();
        } catch (Exception e) {
            log.warn("[{}] could not read position value — liquidity gate open: {}",
                    series.getName(), e.toString());
        }
        if (positionValue == null || positionValue.signum() <= 0) {
            return new BooleanRule(true);
        }
        return new LiquidityRule(new MedianTurnoverIndicator(series, liquidityTurnoverBars),
                series.numOf(positionValue),
                series.numOf(maxPositionPctOfTurnover));
    }

    Rule exitRule() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        ParabolicSarIndicator pSar = new ParabolicSarIndicator(barSeries);
        IsFallingRule pSarIsFallingRule = new IsFallingRule(pSar, 2);

        exitRule = pSarIsFallingRule
                .or(new StopLossRule(closePrice, 2))
               .or(new TrailingStopLossRule(closePrice, DoubleNum.valueOf(2)));

        return exitRule;
    }

    public Double getPEGRatio(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElseGet(null);
        return security.getPegRatio() != null ? security.getPegRatio() : Double.valueOf(0.0);
    }

    public Double getBeta(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElseGet(null);
        return security.getBeta() != null ? security.getBeta() : Double.valueOf(0.0);
    }

    public Double getYoYGrowth(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElseGet(null);
        return security.getYoyGrowth() != null ? security.getYoyGrowth() : Double.valueOf(0.0);
    }

    public Double getDividendYield(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElseGet(null);
        return security.getDividendYield() != null ? security.getDividendYield() : Double.valueOf(0.0);
    }

    public Double getTrailingEps(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElseGet(null);
        return security.getTrailingEps() != null ? security.getTrailingEps() : Double.valueOf(0.0);
    }

    public List<RecommendationTrend> getRecommendationTrends(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElse(null);
        return recommendationTrendRepository.findBySecurityOrderByPeriod(security);
    }

    public RecommendationTrend getRecommendation(String securityId) {
        final Security security = securityRepository.findById(Long.valueOf(securityId)).orElse(null);
        return recommendationTrendRepository.findLatestCurrentTrendBySecurity(security).orElse(null);
    }

}
