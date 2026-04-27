package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.*;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes a single strategy against a list of BarSeries.
 * Extracted from StrategyAnalysis so that @Transactional(REQUIRES_NEW)
 * is honoured by the Spring proxy (private methods are not proxied).
 */
@Service
@Slf4j
public class StrategyExecutor {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    StrategyTradeRepository tradesRepository;

    @Autowired
    StrategyIndicatorValueRepository indicatorValueRepo;

    @Autowired
    StrategiesMap strategiesMap;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(String strategy, List<BarSeries> barSeriesList) throws DataIntegrityViolationException {
        log.info("execute strategy={}, size={}", strategy, barSeriesList.size());
        AtomicReference<FeaturedStrategy> fs = new AtomicReference<>();
        AtomicReference<Double> totalProfit = new AtomicReference<>((double) 0);
        AtomicReference<Double> totalGrossReturn = new AtomicReference<>((double) 0);
        AtomicReference<Date> latestTradeDate = new AtomicReference<>();
        AtomicReference<Strategy> strategyToRun = new AtomicReference<>();

        barSeriesList.forEach(series -> {
            log.info("execute({}, {})", strategy, series.getName());
            try {
                strategyToRun.set(strategiesMap.getStrategyToRun(strategy, series));
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("continue on: " + e);
            }
            Security security = securityRepository.findById(Long.valueOf(series.getName())).orElse(null);
            log.info("Running strategy {} on security {} - {}", strategy, security.getId(), security.getName());
            if (series.getBarData().isEmpty()) {
                log.warn("Something fishy on {}. BarData isEmpty, continues...", series.getName());
                security.setActive(false);
                securityRepository.save(security);
                return;
            }
            fs.set(featuredStrategyRepository.findByNameAndSecurityName(strategy, security.getName()));
            if (fs.get() == null) {
                fs.set(new FeaturedStrategy());
                fs.get().setName(strategy);
                fs.get().setSecurityName(security.getName());
                fs.get().setSecurityDesc(security.getDescription());
            }
            TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategyToRun.get());
            Set<StrategyTrade> strategyTradeList = new HashSet<>();
            StrategyTrade strategyTrade = null;
            for (Position position : tradingRecord.getPositions()) {
                Bar barEntry = series.getBar(position.getEntry().getIndex());
                strategyTrade = new StrategyTrade();
                strategyTrade.setDate(Date.from(barEntry.getEndTime().toInstant()));
                strategyTrade.setType(position.getEntry().getType().name());
                strategyTrade.setPrice(safeBigDecimal(position.getEntry().getPricePerAsset().doubleValue()));
                strategyTrade.setAmount(safeBigDecimal(position.getEntry().getAmount().doubleValue()));
                strategyTradeList.add(strategyTrade);
                Bar barExit = series.getBar(position.getExit().getIndex());
                Date sellDate = Date.from(barExit.getEndTime().toInstant());
                String exitType = position.getExit().getType().name();
                BigDecimal grossProfit = safeBigDecimal(position.getGrossProfit().doubleValue());
                BigDecimal pnl = BigDecimal.ZERO;
                if (!Double.isNaN(position.getGrossReturn().doubleValue())) {
                    pnl = new BigDecimal((position.getGrossReturn().doubleValue() - 1) * 100).setScale(4, BigDecimal.ROUND_DOWN);
                }
                strategyTrade = new StrategyTrade();
                strategyTrade.setDate(sellDate);
                strategyTrade.setType(exitType);
                strategyTrade.setPrice(safeBigDecimal(position.getExit().getPricePerAsset().doubleValue()));
                strategyTrade.setAmount(safeBigDecimal(position.getEntry().getAmount().doubleValue()));
                strategyTrade.setGrossProfit(grossProfit);
                strategyTrade.setPnl(pnl);
                strategyTrade.setFeaturedStrategy(fs.get());
                strategyTradeList.add(strategyTrade);
                latestTradeDate.set(Date.from(barExit.getEndTime().toInstant()));
            }
            if (tradingRecord.getCurrentPosition().isOpened()) {
                Bar barEntry = series.getBar(tradingRecord.getCurrentPosition().getEntry().getIndex());
                strategyTrade = new StrategyTrade();
                strategyTrade.setDate(Date.from(barEntry.getEndTime().toInstant()));
                strategyTrade.setType(tradingRecord.getCurrentPosition().getEntry().getType().name());
                strategyTrade.setPrice(safeBigDecimal(tradingRecord.getCurrentPosition().getEntry().getPricePerAsset().doubleValue()));
                strategyTrade.setAmount(safeBigDecimal(tradingRecord.getCurrentPosition().getEntry().getAmount().doubleValue()));
                strategyTradeList.add(strategyTrade);
                latestTradeDate.set(Date.from(barEntry.getBeginTime().toInstant()));
            }
            fs.get().setPeriod(getPeriod(series));
            fs.get().setLatestTrade(latestTradeDate.get());
            totalProfit.set(new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue());
            if (!Double.isNaN(totalProfit.get())) {
                fs.get().setTotalProfit(new BigDecimal(totalProfit.get()).setScale(4, BigDecimal.ROUND_DOWN));
            } else {
                fs.get().setTotalProfit(BigDecimal.ZERO);
            }
            totalGrossReturn.set(new ProfitCriterion().calculate(series, tradingRecord).doubleValue());
            if (!Double.isNaN(totalGrossReturn.get())) {
                fs.get().setTotalGrossReturn(new BigDecimal(totalGrossReturn.get()).setScale(4, BigDecimal.ROUND_DOWN));
            } else {
                fs.get().setTotalGrossReturn(BigDecimal.ZERO);
            }
            fs.get().setNumberofTrades(new BigDecimal(new NumberOfPositionsCriterion().calculate(series, tradingRecord).doubleValue()).intValue());
            double profitableTradesRatio = PositionsRatioCriterion.WinningPositionsRatioCriterion().calculate(series, tradingRecord).doubleValue();
            if (!Double.isNaN(profitableTradesRatio)) {
                fs.get().setProfitableTradesRatio(new BigDecimal(profitableTradesRatio * 100).setScale(2, RoundingMode.DOWN));
            }
            double maximumDrawdownCriterion = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
            if (!Double.isNaN(maximumDrawdownCriterion)) {
                fs.get().setMaxDD(new BigDecimal(maximumDrawdownCriterion * 100).setScale(2, RoundingMode.DOWN));
            } else {
                fs.get().setMaxDD(BigDecimal.ZERO);
            }
            fs.get().setOpen(tradingRecord.getCurrentPosition().isOpened());
            double sqn = new SqnCriterion().calculate(series, tradingRecord).doubleValue();
            fs.get().setSqn(Double.isNaN(sqn) ? BigDecimal.ZERO : new BigDecimal(sqn));
            double expectancy = new ExpectancyCriterion().calculate(series, tradingRecord).doubleValue();
            fs.get().setExpectency(Double.isNaN(expectancy) ? BigDecimal.ZERO : new BigDecimal(expectancy));
            AtomicReference<FeaturedStrategy> fsRes = new AtomicReference<>(featuredStrategyRepository.save(fs.get()));
            List<StrategyTrade> existingStrategyTrades = tradesRepository.findByFeaturedStrategyId(fsRes.get().getId());
            if (!existingStrategyTrades.isEmpty()) {
                tradesRepository.deleteAllInBatch(existingStrategyTrades);
            }
            strategyTradeList.forEach(st -> st.setFeaturedStrategy(fsRes.get()));
            tradesRepository.saveAll(strategyTradeList);
            List<StrategyIndicatorValue> existIv = indicatorValueRepo.findByFeaturedStrategyId(fsRes.get().getId());
            if (!existIv.isEmpty()) {
                indicatorValueRepo.deleteAllInBatch(existIv);
            }
            List<StrategyIndicatorValue> ivList = strategiesMap.getIndicatorValues(strategy, null);
            ivList.forEach(iv -> iv.setFeaturedStrategy(fsRes.get()));
            indicatorValueRepo.saveAll(ivList);
        });
    }

    private String getPeriod(BarSeries series) {
        StringBuilder sb = new StringBuilder();
        if (!series.getBarData().isEmpty()) {
            Bar firstBar = series.getFirstBar();
            Bar lastBar = series.getLastBar();
            sb.append(firstBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            sb.append("-");
            sb.append(lastBar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return sb.toString();
    }

    private BigDecimal safeBigDecimal(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
