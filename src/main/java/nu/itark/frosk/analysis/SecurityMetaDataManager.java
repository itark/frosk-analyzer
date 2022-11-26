package nu.itark.frosk.analysis;

import com.coinbase.exchange.model.Granularity;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingInt;

@Component
@Slf4j
public class SecurityMetaDataManager {

    @Autowired
    BarSeriesService timeSeriesService;

    @Autowired
    DataSetRepository datasetRepository;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;


    /**
     * Gets all featured strategies, hence per all strategies and all securities.
     *
     * @return
     */
    public List<FeaturedStrategyDTO> getFeaturedStrategies() {
        List<FeaturedStrategyDTO> returnList = new ArrayList<>();
        DataSet dataset = datasetRepository.findByName(Database.COINBASE.name());
        List<String> strategies = StrategiesMap.buildStrategiesMap();

        log.info("strategies:{}",strategies.size());
        log.info("dataset.getSecurities():{}",dataset.getSecurities().size());
        log.info("featuredStrategyRepository.findAll().size():{}",featuredStrategyRepository.findAll().size());

/*
        strategies.forEach(strategyName -> {
            dataset.getSecurities().forEach(security -> {
                FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, security.getName());
                returnList.add(getDTO(fs));
            });
        });
*/
        featuredStrategyRepository.findAll().forEach(fs->{
                returnList.add(getDTO(fs));
        });

        log.info("returnList.size():{}",returnList.size());

        return returnList;
    }

    public List<SecurityDTO> getSecurityMetaData() {
        List<SecurityDTO> securityDTOList = new ArrayList<SecurityDTO>();
        DataSet dataset = datasetRepository.findByName(Database.COINBASE.name());

        dataset.getSecurities().forEach(s -> {
            SecurityDTO securityDTO = new SecurityDTO(s.getName());
            addMetaData(securityDTO);
            securityDTOList.add(securityDTO);
        });

        return securityDTOList;
    }

    private void addMetaData(SecurityDTO securityDTO) {
        securityDTO.setOneDayPercent(getBarPercent(securityDTO.getName(),1));
        securityDTO.setOneWeekPercent(getBarPercent(securityDTO.getName(),7));
        securityDTO.setOneMonthPercent(getBarPercent(securityDTO.getName(),30));
        securityDTO.setThreeMonthPercent(getBarPercent(securityDTO.getName(),90));
        securityDTO.setSixMonthPercent(getBarPercent(securityDTO.getName(),180));
    }

    protected BigDecimal getBarPercent(String securityName, int nrOfBars) {
        BarSeries timeSeries = timeSeriesService.getDataSet(securityName, false);
        //Sanitycheck
        if (timeSeries.getBarCount() <= nrOfBars) {
            return null;
        }
        Num entryOpen = timeSeries.getBar((timeSeries.getBarCount()-1) - nrOfBars).getOpenPrice();
        Num exitOpen = timeSeries.getLastBar().getOpenPrice();
        Num grossProfit = exitOpen.minus(entryOpen);
        Num pnl = grossProfit.dividedBy(entryOpen);
        if (pnl.isNaN()) {
            return null;
        } else {
            return BigDecimal.valueOf(pnl.doubleValue()).round(new MathContext(2));
        }
    }

    public FeaturedStrategyDTO getDTO(FeaturedStrategy fs) {
        FeaturedStrategyDTO dto = new FeaturedStrategyDTO();
        if (Objects.isNull(fs)) {
            return dto;
        }
        List<IndicatorValueDTO> indicatorValues = new ArrayList<>();
        dto.setName(fs.getName());
        dto.setSecurityName(fs.getSecurityName());
        dto.setIcon(IconManager.getIconUrl(fs.getSecurityName()));
        dto.setTotalProfit(fs.getTotalProfit());
        dto.setNumberOfTicks(fs.getNumberOfTicks());
        dto.setAverageTickProfit(fs.getAverageTickProfit());
        if (Objects.nonNull(fs.getProfitableTradesRatio())) {
            dto.setProfitableTradesRatio(fs.getProfitableTradesRatio().toPlainString());
        } else {
            dto.setProfitableTradesRatio("empty");
        }
        dto.setMaxDD(fs.getMaxDD());
        dto.setRewardRiskRatio(fs.getRewardRiskRatio());
        dto.setTotalTranactionCost(fs.getTotalTransactionCost());
        dto.setBuyAndHold(fs.getBuyAndHold());
        dto.setTotalProfitVsButAndHold(fs.getTotalProfitVsButAndHold());
        dto.setPeriod(fs.getPeriod());
        if (Objects.nonNull(fs.getLatestTrade())) {
            dto.setLatestTrade(fs.getLatestTrade().toString());
        } else {
            dto.setLatestTrade("empty");
        }
        dto.setIsOpen(String.valueOf(fs.isOpen()));
        dto.setNumberofTrades(fs.getNumberofTrades());
        fs.getIndicatorValues().forEach(siv -> {
            indicatorValues.add(new IndicatorValueDTO(siv.getDate(),siv.getValue(), siv.getIndicator()));
        });
        dto.setIndicatorValues(indicatorValues);
        dto.setTrades(convert(fs.getTrades()));

        return dto;
    }

    private Set<TradeDTO> convert(Set<StrategyTrade> tradeList) {
        Set<TradeDTO> trades = new HashSet<TradeDTO>();
        tradeList.forEach(trade -> {
            TradeDTO tradee = new TradeDTO();
            tradee.setId(trade.getId());
            tradee.setDate(trade.getDate().toInstant().toEpochMilli());
            tradee.setDateReadable(DateFormatUtils.format(trade.getDate(), "yyyy-MM-dd"));
            tradee.setPrice(BigDecimal.valueOf(trade.getPrice().doubleValue()));
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            if (Objects.nonNull(trade.getGrossProfit())) {
                tradee.setGrossProfit(BigDecimal.valueOf(trade.getGrossProfit().doubleValue()));
            }
            if (Objects.nonNull(trade.getPnl())) {
                tradee.setPnl(BigDecimal.valueOf(trade.getPnl().doubleValue()));
            }
            trades.add(tradee);
        });
        return trades;
    }

}
