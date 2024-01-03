package nu.itark.frosk.strategies.filter;

import nu.itark.frosk.analysis.OpenFeaturedStrategyDTO;
import nu.itark.frosk.analysis.SecurityMetaDataManager;
import nu.itark.frosk.analysis.TradeDTO;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StrategyFilter {


    @Value("${frosk.criteria.sqn}")
    private BigDecimal sqn;

    @Autowired
    StrategyTradeRepository tradesRepository;
    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    SecurityMetaDataManager securityMetaDataManager;

    public List<OpenFeaturedStrategyDTO> getOpenSmartSignals() {
        List<OpenFeaturedStrategyDTO>  openFeaturedStrategyDTOList = new ArrayList<>();
        BigDecimal aboveProfTradesRatio= new BigDecimal(0.5);
        Integer aboveNrOfTrades= 10;
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findSmartSignals(aboveProfTradesRatio,aboveNrOfTrades);
        OpenFeaturedStrategyDTO dto;
        fsList.forEach(fs-> {
            openFeaturedStrategyDTOList.add(getDTO(fs));
        });
        return openFeaturedStrategyDTOList;
    }

    public OpenFeaturedStrategyDTO getDTO(FeaturedStrategy fs) {
        Long nrOfBars = DateTimeManager.nrOfDays(DateTimeManager.convertToLocalDateTime(fs.getLatestTrade()) ,
                LocalDateTime.now());
        return OpenFeaturedStrategyDTO.builder()
                .name(fs.getName().replace("Strategy",""))
                .securityName(fs.getSecurityName())
                .latestTrade(DateFormatUtils.format(fs.getLatestTrade(), "yyyy-MM-dd"))
                .totalProfit(securityMetaDataManager.getBarPercent(fs.getSecurityName(), nrOfBars.intValue()))
                .close(securityMetaDataManager.getLatestClose(fs.getSecurityName()))
                .build();
    }

    public List<TradeDTO> getLongTradesAllStrategies(String strategyName) {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.BUY));
    }

    public List<TradeDTO> getShortTrades(String strategyName) {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.SELL));
    }

    public List<TradeDTO> getLongTradesAllStrategies() {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.BUY));
    }

    public List<TradeDTO> getShortTradesAllStrategies() {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.SELL));
    }

    public List<TradeDTO> getTrades(String security, String strategy){
        FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security);
        return convert(tradesRepository.findByFeaturedStrategy(fs));
    }

    private List<StrategyTrade> getTradesForStrategies(List<FeaturedStrategy> fsList, org.ta4j.core.Trade.TradeType orderType) {
        List<StrategyTrade> strategyTradeListList = new ArrayList<>();
        fsList.forEach(featuredStrategy -> {
            final List<StrategyTrade> byFeaturedStrategy = tradesRepository.findByFeaturedStrategy(featuredStrategy);
            if (byFeaturedStrategy.isEmpty()) return;
            StrategyTrade latestTrade = Collections.max(byFeaturedStrategy, Comparator.comparing(StrategyTrade::getDate));
            if (latestTrade.getType().equals(orderType.name())) {
                strategyTradeListList.add(latestTrade);
            }
        });
        return strategyTradeListList;
    }

    private List<TradeDTO> convert(List<StrategyTrade> tradeList) {
        List<TradeDTO> trades = new ArrayList<TradeDTO>();
        tradeList.forEach(trade -> {
            TradeDTO tradee = new TradeDTO();
            tradee.setId(trade.getId());
            tradee.setDate(trade.getDate().toInstant().toEpochMilli());
            tradee.setDateReadable(DateFormatUtils.format(trade.getDate(), "yyyy-MM-dd"));
            tradee.setPrice(BigDecimal.valueOf(trade.getPrice().doubleValue()));
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            trades.add(tradee);
        });
        return trades;
    }

}
