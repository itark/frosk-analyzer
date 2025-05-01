package nu.itark.frosk.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.HedgeIndex;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.HedgeIndexRepository;
import nu.itark.frosk.repo.StrategyTradeRepository;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class HedgeIndexService {

    final FeaturedStrategyRepository featuredStrategyRepository;
    final StrategyTradeRepository strategyTradeRepository;
    final HedgeIndexRepository hedgeIndexRepository;

    @Transactional
    public void update() {
        updateHedgeIndex("VIXStrategy", "^VIX", this::convertToVixHedgeIndexes);
        updateHedgeIndex("CrudeOilStrategy", "CL=F", this::convertToCrudeOilStrategyHedgeIndexes);
    }

    private void updateHedgeIndex(String strategyName, String securityName, java.util.function.Function<List<StrategyTrade>, List<HedgeIndex>> converter) {
        FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
        if (featuredStrategy != null) {
            Optional<HedgeIndex> hedgeIndexLatestDate = hedgeIndexRepository.findTopByIndicatorOrderByDateDesc(securityName);
            Date latestDate;
            List<StrategyTrade> strategyTrades;
            if (hedgeIndexLatestDate.isPresent()) {
                latestDate = hedgeIndexLatestDate.get().getDate();
                 strategyTrades = strategyTradeRepository.findByFeaturedStrategyIdAndDateAfter(featuredStrategy.getId(), latestDate);
            } else {
                strategyTrades = strategyTradeRepository.findByFeaturedStrategyId(featuredStrategy.getId());
            }
            List<HedgeIndex> hedgeIndexList = converter.apply(strategyTrades);
            hedgeIndexRepository.saveAllAndFlush(hedgeIndexList);
            log.info("HedgeIndex updated for: {} with strategyTrades:{}",strategyName,strategyTrades.size());
        } else {
            log.error("Warning: FeaturedStrategy not found for name: " + strategyName + ", security: " + securityName);
        }
    }

    private List<HedgeIndex> convertToVixHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Volatility");
            hedgeIndex.setIndicator("VIX");
            hedgeIndex.setRuleDesc("VIX > 25 and rising");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }


    private List<HedgeIndex> convertToCrudeOilStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Commodities");
            hedgeIndex.setIndicator("Crude oil");
            hedgeIndex.setRuleDesc("Drops >5% in last 5 days");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    /**
     * Return risk, hence if risk < threshold go long
     * if risk > threshold, go short.
     *
     * @param indexDate
     * @param name
     * @return true id risk
     */
    public boolean risk(ZonedDateTime indexDate, String name) {
       //log.info("name:{}",name);
        final List<HedgeIndex> hedgeIndexByDateList = hedgeIndexRepository.findByDate(Date.from(indexDate.toInstant()));
        log.info("hedgeIndexByDateList:{}",hedgeIndexByDateList);
        int risks = countRisksIndicators(hedgeIndexByDateList);
        if (risks > 2) {
            log.info("risks:{}",risks);
            return true;
        } else {
            return false;
        }

    }


    private int countRisksIndicators(List<HedgeIndex> hedgeIndexList) {
        int count = 0;
        for (HedgeIndex hedgeIndex : hedgeIndexList) {
            if (hedgeIndex.getRisk()) {
                count++;
            }
        }
        return count;
    }

}
