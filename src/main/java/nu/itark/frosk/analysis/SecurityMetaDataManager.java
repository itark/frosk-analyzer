package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.*;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

@Component
@Slf4j
public class SecurityMetaDataManager {

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    SecurityPriceRepository securityPriceRepository;

    @Autowired
    StrategiesMap strategiesMap;

    @Value("${frosk.database.only:YAHOO}")
    private String databaseOnly;

    public List<SecurityDTO> getSecurityMetaData(String databaseOnly) {
        List<SecurityDTO> securityDTOList = new ArrayList<SecurityDTO>();
        List<Security> securities = securityRepository.findByDatabase(databaseOnly);

        securities.forEach(s -> {
           //s log.info("s:{}",s);
            SecurityDTO securityDTO = SecurityDTO.builder()
                    .name(s.getName())
                    .desc(s.getDescription())
                    .yoyGrowth(s.getYoyGrowth() != null ? Math.round(s.getYoyGrowth() * 100.0) / 100.0 : null)
                    .pegRatio(s.getPegRatio() != null ? Math.round(s.getPegRatio() * 100.0) / 100.0 : null)
                    .beta(s.getBeta() != null ? Math.round(s.getBeta() * 100.0) / 100.0 : null)
                    .trailingEps(s.getTrailingEps() != null ? Math.round(s.getTrailingEps() * 100.0) / 100.0 : null)
                    .forwardEps(s.getForwardEps() != null ? Math.round(s.getForwardEps() * 100.0) / 100.0 : null)
                    .trailingPe(s.getTrailingPe() != null ? Math.round(s.getTrailingPe() * 100.0) / 100.0 : null)
                    .forwardPe(s.getForwardPe() != null ? Math.round(s.getForwardPe() * 100.0) / 100.0 : null)
                    .build();
           // addMetaData(securityDTO);
            securityDTOList.add(securityDTO);
        });

        return securityDTOList;
    }

    private void addMetaData(SecurityDTO securityDTO) {
        securityDTO.setOneDayPercent(getBarPercent(securityDTO.getName(), 1));
        securityDTO.setOneWeekPercent(getBarPercent(securityDTO.getName(), 6));
        securityDTO.setOneMonthPercent(getBarPercent(securityDTO.getName(), 29));
        securityDTO.setThreeMonthPercent(getBarPercent(securityDTO.getName(), 89));
        securityDTO.setSixMonthPercent(getBarPercent(securityDTO.getName(), 179));
        securityDTO.setBestStrategy(getBestStrategy(securityDTO.getName()).getName());
    }

    private Strategy getBestStrategy(String securityName) {
        BarSeries barSeries = barSeriesService.getDataSet(securityName, false, false);
        List<Strategy> strategies = strategiesMap.getStrategies(barSeries);
        AnalysisCriterion profitCriterion = new ReturnCriterion();
        BarSeriesManager timeSeriesManager = new BarSeriesManager(barSeries);
        return profitCriterion.chooseBest(timeSeriesManager, new ArrayList<Strategy>(strategies));
    }

    public BigDecimal getBarPercent(String securityName, int nrOfBars) {
        BarSeries timeSeries = barSeriesService.getDataSet(securityName, false, false);
        //Sanitycheck
        if (timeSeries.getBarCount() <= nrOfBars || nrOfBars == 0) {
            return null;
        }
        Num entryOpen = timeSeries.getBar((timeSeries.getBarCount()) - nrOfBars).getOpenPrice();
        Num exitClose = timeSeries.getLastBar().getClosePrice();
        Num grossProfit = exitClose.minus(entryOpen);
        Num pnl = grossProfit.dividedBy(entryOpen).multipliedBy(timeSeries.numOf(100));
        if (pnl.isNaN()) {
            return null;
        } else {
            return BigDecimal.valueOf(pnl.doubleValue()).round(new MathContext(2));
        }
    }

    public BigDecimal getBarGrossProfit(String securityName, int nrOfBars) {
        BarSeries timeSeries = barSeriesService.getDataSet(securityName, false, false);
        //Sanitycheck
        if (timeSeries.getBarCount() <= nrOfBars || nrOfBars == 0) {
            return null;
        }
        Num entryOpen = timeSeries.getBar((timeSeries.getBarCount()) - nrOfBars).getOpenPrice();
        Num exitClose = timeSeries.getLastBar().getClosePrice();
        Num grossProfit = exitClose.minus(entryOpen);
        return BigDecimal.valueOf(grossProfit.doubleValue()).round(new MathContext(2));
    }

    public BigDecimal getPrice(String securityName, Date latestTrade) {
        final Security security = securityRepository.findByName(securityName);
        final SecurityPrice securityPrice = securityPriceRepository.findBySecurityIdAndTimestamp(security.getId(), latestTrade);
        return securityPrice.getOpen();
    }

    public BigDecimal getLatestClose(String securityName) {
        final Security security = securityRepository.findByName(securityName);
        final SecurityPrice securityPrice = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
        return securityPrice.getClose();
    }

    public FeaturedStrategyDTO getDTO(FeaturedStrategy fs, boolean includeIndicatorValues) {
        FeaturedStrategyDTO dto = new FeaturedStrategyDTO();
        if (Objects.isNull(fs)) {
            return dto;
        }
         List<IndicatorValueDTO> indicatorValues = new ArrayList<>();
        dto.setName(fs.getName().replace("Strategy", ""));
        dto.setSecurityName(fs.getSecurityName());
        dto.setIcon(IconManager.getIconUrl(fs.getSecurityName()));
        dto.setTotalProfit(fs.getTotalProfit());
        dto.setTotalGrossReturn(fs.getTotalGrossReturn());
        dto.setNumberOfTicks(fs.getNumberOfTicks());
        dto.setAverageTickProfit(fs.getAverageTickProfit());
        if (Objects.nonNull(fs.getProfitableTradesRatio())) {
            dto.setProfitableTradesRatio(fs.getProfitableTradesRatio().toPlainString());
        } else {
            dto.setProfitableTradesRatio("empty");
        }
        dto.setMaxDD(fs.getMaxDD());
        dto.setSqn(fs.getSqn());
        dto.setExpectancy(fs.getExpectency());
        dto.setTotalTransactionCost(fs.getTotalTransactionCost());
        dto.setPeriod(fs.getPeriod());
        if (Objects.nonNull(fs.getLatestTrade())) {
            dto.setLatestTrade(fs.getLatestTrade().toString());
        } else {
            dto.setLatestTrade("empty");
        }
        dto.setIsOpen(String.valueOf(fs.isOpen()));
        dto.setTotalTransactionCost(fs.getTotalTransactionCost());
        dto.setNumberofTrades(fs.getNumberofTrades());

        if (includeIndicatorValues) {
            fs.getIndicatorValues().forEach(siv -> {
                indicatorValues.add(new IndicatorValueDTO(siv.getDate(), siv.getValue(), siv.getIndicator()));
            });
            dto.setIndicatorValues(indicatorValues);
        }

        dto.setTrades(convert(fs.getStrategyTrades()));

        return dto;
    }

    private Set<TradeDTO> convert(Set<StrategyTrade> tradeList) {
        Set<TradeDTO> trades = new HashSet<TradeDTO>();
        tradeList.forEach(trade -> {
            TradeDTO tradee = new TradeDTO();
            tradee.setId(trade.getId());
            tradee.setDate(trade.getDate().toInstant().toEpochMilli());
            tradee.setDateReadable(DateFormatUtils.format(trade.getDate(), "yyyy-MM-dd"));
            tradee.setPrice(trade.getPrice());
            tradee.setAmount(trade.getAmount());
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            if (Objects.nonNull(trade.getGrossProfit())) {
                tradee.setGrossProfit(trade.getGrossProfit());
            }
            if (Objects.nonNull(trade.getPnl())) {
                tradee.setPnl(trade.getPnl());
            }
            trades.add(tradee);
        });
        return trades;
    }

    public BigDecimal getPrice(FeaturedStrategy fs) {
        Comparator<StrategyTrade> date = (c1, c2) -> Long.valueOf(c1.getDate().getTime()).compareTo(c2.getDate().getTime());
        return fs.getStrategyTrades().stream()
                .sorted(date.reversed())
                .map(st -> st.getPrice())
                .findFirst().orElseThrow();
    }

    public StrategyTrade getLastBuyTrade(FeaturedStrategy fs, int offset) {
        Comparator<StrategyTrade> date = (c1, c2) -> Long.valueOf(c1.getDate().getTime()).compareTo(c2.getDate().getTime());
        final List<StrategyTrade> strategyTradeList = fs.getStrategyTrades().stream()
                .sorted(date).toList();
        return strategyTradeList.get(strategyTradeList.size()-offset);
    }

}
