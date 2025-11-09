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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * Regime Classification:
 * 0–2 points: Risk-On → Go long S&P/NASDAQ/Dow
 * 3–5 points: Neutral → Hedge / reduced exposure
 * 6–12 points: Risk-Off → Short / rotate to defensive assets
 *
 - **0–3 points — Strong Risk-On**
 - Target equity exposure: **80–100%** (long S&P/NASDAQ/Dow)
 - Hedge: minimal (cash or small put protection)
 - Preferred: high-beta growth, momentum names

 - **4–7 points — Mild/Transition (Cautious Risk-On / Neutral)**
 - Target equity exposure: **40–70%**
 - Hedge: partial protection (collars, protective puts on core names)
 - Preferred: selective longs, reduce leverage, trim weakest performers

 - **8–11 points — Neutral / Defensive**
 - Target equity exposure: **10–40%**
 - Hedge: long Gold, long VIX exposure, increase cash
 - Preferred: defensive sectors, high-quality dividend names

 - **12+ points — Strong Risk-Off**
 - Target equity exposure: **0–10%** or net short
 - Hedge: long VIX/volatility products, long Gold, short indices (bear puts, inverse ETFs)
 - Preferred: capitalize on protective trades and volatility spreads
 *
 *
 */
public class HedgeIndexService {

    @Value("${frosk.hedge.criteria.risk.threshold}")
    private int riskThreshold;

    final FeaturedStrategyRepository featuredStrategyRepository;
    final StrategyTradeRepository strategyTradeRepository;
    final HedgeIndexRepository hedgeIndexRepository;

    @Transactional
    public void update() {
        updateHedgeIndex("VIXStrategy", "^VIX", this::convertToVixHedgeIndexes);
        updateHedgeIndex("VVIXStrategy", "^VVIX", this::convertToVVixHedgeIndexes);
        updateHedgeIndex("CrudeOilStrategy", "CL=F", this::convertToCrudeOilStrategyHedgeIndexes);
        updateHedgeIndex("GoldStrategy", "GC=F", this::convertToGoldStrategyHedgeIndexes);
        updateHedgeIndex("SP500Strategy", "^GSPC", this::convertToSP500StrategyHedgeIndexes);
        updateHedgeIndex("NasdaqVsSPStrategy", "^IXIC", this::convertToNasdaqVsSPStrategyHedgeIndexes);
    }

    private void updateHedgeIndex_OLD(String strategyName, String securityName, java.util.function.Function<List<StrategyTrade>, List<HedgeIndex>> converter) {
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
            //log.info("HedgeIndex updated for: {} with strategyTrades:{}",strategyName,strategyTrades.size());
        } else {
            log.error("Warning: FeaturedStrategy not found for name: " + strategyName + ", security: " + securityName);
        }
    }

    private void updateHedgeIndex(String strategyName, String securityName,
                                  java.util.function.Function<List<StrategyTrade>, List<HedgeIndex>> converter) {
        FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
        if (featuredStrategy != null) {
            Optional<HedgeIndex> hedgeIndexLatestDate = hedgeIndexRepository.findTopByIndicatorOrderByDateDesc(securityName);
            Date latestDate;
            List<StrategyTrade> strategyTrades;

            if (hedgeIndexLatestDate.isPresent()) {
               // log.info("hedgeIndexLatestDate: {}", hedgeIndexLatestDate);
                latestDate = hedgeIndexLatestDate.get().getDate();
                strategyTrades = strategyTradeRepository.findByFeaturedStrategyIdAndDateGreaterThan(
                        featuredStrategy.getId(), latestDate);
            } else {
                strategyTrades = strategyTradeRepository.findByFeaturedStrategyId(featuredStrategy.getId());
            }

            // Only save if there are new trades to process
            if (!strategyTrades.isEmpty()) {
                List<HedgeIndex> hedgeIndexList = converter.apply(strategyTrades);
                hedgeIndexRepository.saveAllAndFlush(hedgeIndexList);
                //log.info("HedgeIndex updated for: {} with strategyTrades: {}", strategyName, strategyTrades.size());
            } else {
                //log.info("No new trades to process for: {}", strategyName);
            }
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
            hedgeIndex.setIndicator("^VIX");
            hedgeIndex.setRuleDesc("VIX > 25 and rising");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToVVixHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("VIX Volatility");
            hedgeIndex.setIndicator("^VVIX");
            hedgeIndex.setRuleDesc("VVIX > 110 and rising");
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
            hedgeIndex.setIndicator("CL=F");
            hedgeIndex.setRuleDesc("Crude oil drops >5% in last 5 days");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToGoldStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Commodities");
            hedgeIndex.setIndicator("CL=F");
            hedgeIndex.setRuleDesc("Gold breaks above 10-day high");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToSP500StrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Equities");
            hedgeIndex.setIndicator("^GSPC");
            hedgeIndex.setRuleDesc("S&P 500 below 200-day MA");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToNasdaqVsSPStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Equities");
            hedgeIndex.setIndicator("^IXIC");
            hedgeIndex.setRuleDesc("NASDAQ 30-day return < S&P 30-day return");
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
     * @return true id risk
     */
    public boolean risk(ZonedDateTime indexDate) {
        final List<HedgeIndex> hedgeIndexByDateList = hedgeIndexRepository.findByDate(Date.from(indexDate.toInstant()));
        int risks = countRisksIndicators(hedgeIndexByDateList);
        //log.info("risks:{}",risks);
        if (risks > riskThreshold) {
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
