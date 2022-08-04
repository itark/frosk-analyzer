package nu.itark.frosk.strategies.filter;

import nu.itark.frosk.dataset.Trade;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.TradesRepository;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class StrategyFilter {

    @Autowired
    TradesRepository tradesRepository;
    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    public List<Trade> getLongTradesAllStrategies(String strategyName) {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.BUY));
    }

    public List<Trade> getShortTrades(String strategyName) {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName(strategyName);
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.SELL));
    }

    public List<Trade> getLongTradesAllStrategies() {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.BUY));
    }

    public List<Trade> getShortTradesAllStrategies() {
        List<FeaturedStrategy> fsList = featuredStrategyRepository.findAll();
        return convert(getTradesForStrategies(fsList, org.ta4j.core.Trade.TradeType.SELL));
    }

    public List<Trade> getTrades(String security, String strategy){
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

    private List<Trade> convert(List<StrategyTrade> tradeList) {
        List<Trade> trades = new ArrayList<Trade>();
        tradeList.forEach(trade -> {
            Trade tradee = new Trade();
            tradee.setId(trade.getId());
            tradee.setDate(trade.getDate().toInstant().toEpochMilli());
            tradee.setDateReadable(DateFormatUtils.format(trade.getDate(), "yyyy-MM-dd HH:mm:ss"));
            tradee.setPrice(trade.getPrice().longValue());
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            trades.add(tradee);
        });
        return trades;
    }

}
