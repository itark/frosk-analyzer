package nu.itark.frosk.strategies.filter;

import nu.itark.frosk.analysis.*;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.repo.TopStrategy;
import nu.itark.frosk.util.DateTimeManager;
import nu.itark.frosk.util.FroskUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.*;

import static java.math.RoundingMode.FLOOR;

@Service
public class StrategyFilter {

    @Value("${frosk.criteria.sqn}")
    private BigDecimal sqn;

    @Value("${frosk.criteria.expectency}")
    private BigDecimal expectency;

    @Value("${frosk.criteria.profitable.ratio}")
    private BigDecimal profitableRatio;

    @Value("${frosk.criteria.numberOfTrades}")
    private Integer numberOfTrades;

    @Value("${frosk.criteria.open}")
    private Boolean isOpen;


    @Autowired
    StrategyTradeRepository tradesRepository;
    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    SecurityMetaDataManager securityMetaDataManager;

    public List<FeaturedStrategyDTO> getFeaturedStrategies() {
        List<FeaturedStrategyDTO> returnList = new ArrayList<>();
        featuredStrategyRepository.findAll().forEach(fs->{
            returnList.add(getDTO(fs, false));
        });
        return returnList;
    }

    public List<FeaturedStrategyDTO> getTopFeaturedStrategies() {
        List<FeaturedStrategyDTO> returnList = new ArrayList<>();
        featuredStrategyRepository.findTopStrategies(profitableRatio, numberOfTrades, sqn, expectency, isOpen ).forEach(fs->{
            returnList.add(getDTO(fs, false));
        });
        return returnList;
    }

    public List<TopStrategyDTO> findBestPerformingStrategies() {
        List<TopStrategyDTO> returnList = new ArrayList<>();
        featuredStrategyRepository.findBestPerformingStrategies().forEach(fs->{
            returnList.add(getDTO(fs));
        });
        return returnList;
    }

    public List<OpenFeaturedStrategyDTO> getOpenSmartSignals() {
        List<OpenFeaturedStrategyDTO>  openFeaturedStrategyDTOList = new ArrayList<>();
      //  BigDecimal aboveProfTradesRatio= new BigDecimal(0.5);
       // Integer aboveNrOfTrades= 10;
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findSmartSignals(profitableRatio, numberOfTrades, sqn, expectency, true);
        fsList.forEach(fs-> {
            openFeaturedStrategyDTOList.add(getDTO(fs));
        });
        return openFeaturedStrategyDTOList;
    }

    public OpenFeaturedStrategyDTO getDTO(FeaturedStrategy fs) {
        BigDecimal openPrice = securityMetaDataManager.getPrice(fs);
        BigDecimal closePrice =  securityMetaDataManager.getLatestClose(fs.getSecurityName());
        return OpenFeaturedStrategyDTO.builder()
                .name(fs.getName().replace("Strategy",""))
                .securityName(fs.getSecurityName())
                .openPrice(openPrice)
                .openTradeDate(DateFormatUtils.format(fs.getLatestTrade(), "yyyy-MM-dd"))
                .totalProfit(FroskUtil.getPercentage(openPrice, closePrice))
                .closePrice(closePrice)
                .build();
    }

    public TopStrategyDTO getDTO(TopStrategy ts) {
        return TopStrategyDTO.builder()
                .name(ts.getName())
                .totalProfit(BigDecimal.valueOf(ts.getTotalProfit().doubleValue()).round(new MathContext(2)))
                .sqn(ts.getSqn().abs().doubleValue() < 100 ? BigDecimal.valueOf(ts.getSqn().doubleValue()).round(new MathContext(2)): BigDecimal.valueOf(0L))
                .sqnRaw(ts.getSqn())
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
            tradee.setAmount(BigDecimal.valueOf(trade.getAmount().doubleValue()));
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            trades.add(tradee);
        });
        return trades;
    }

    public FeaturedStrategyDTO getDTO(FeaturedStrategy fs, boolean includeIndicatorValues) {
        FeaturedStrategyDTO dto = new FeaturedStrategyDTO();
        if (Objects.isNull(fs)) {
            return dto;
        }
        List<IndicatorValueDTO> indicatorValues = new ArrayList<>();
        dto.setName(fs.getName().replace("Strategy",""));
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
                indicatorValues.add(new IndicatorValueDTO(siv.getDate(),siv.getValue(), siv.getIndicator()));
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



}
