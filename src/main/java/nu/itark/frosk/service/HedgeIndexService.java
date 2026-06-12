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
        updateHedgeIndex("EURUSDStrategy", "EURUSD=X", this::convertToEURUSDStrategyHedgeIndexes);
        updateHedgeIndex("USDJPYStrategy", "JPY=X", this::convertToUSDJPYStrategyHedgeIndexes);
        updateHedgeIndex("AUDUSDStrategy", "AUDUSD=X", this::convertToAUDUSDStrategyHedgeIndexes);
        updateHedgeIndex("DXYStrategy", "DX-Y.NYB", this::convertToDXYStrategyHedgeIndexes);
        updateHedgeIndex("VSTOXXStrategy", "^V2TX", this::convertToVSTOXXStrategyHedgeIndexes);
        updateHedgeIndex("OMXvsSTOXX50Strategy", "^STOXX50E", this::convertToOMXvsSTOXX50StrategyHedgeIndexes);
        updateHedgeIndex("TreasuryYieldStrategy", "^TNX", this::convertToTreasuryYieldStrategyHedgeIndexes);
        updateHedgeIndex("YieldCurveSpreadStrategy", "^TNX", this::convertToYieldCurveSpreadStrategyHedgeIndexes);
        clearCache(); // invalidate after data update
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
            } else {
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
            hedgeIndex.setIndicator("GC=F");
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

    private List<HedgeIndex> convertToEURUSDStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("FX");
            hedgeIndex.setIndicator("EURUSD=X");
            hedgeIndex.setRuleDesc("EUR/USD drops >3% in last 10 days");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToUSDJPYStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("FX");
            hedgeIndex.setIndicator("JPY=X");
            hedgeIndex.setRuleDesc("USD/JPY drops >2% in last 5 days (JPY strengthening)");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToAUDUSDStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("FX");
            hedgeIndex.setIndicator("AUDUSD=X");
            hedgeIndex.setRuleDesc("AUD/USD drops >2% in last 5 days");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToDXYStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("FX");
            hedgeIndex.setIndicator("DX-Y.NYB");
            hedgeIndex.setRuleDesc("DXY > 105 and rising");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToVSTOXXStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Volatility");
            hedgeIndex.setIndicator("^V2TX");
            hedgeIndex.setRuleDesc("VSTOXX > 25 and rising");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToOMXvsSTOXX50StrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Equities");
            hedgeIndex.setIndicator("^STOXX50E");
            hedgeIndex.setRuleDesc("OMXS30 30-day return < STOXX50 30-day return by >3pp");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToTreasuryYieldStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Interest Rates");
            hedgeIndex.setIndicator("^TNX");
            hedgeIndex.setRuleDesc("10Y Treasury yield > 4.5% and rising");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    private List<HedgeIndex> convertToYieldCurveSpreadStrategyHedgeIndexes(List<StrategyTrade> strategyTrades) {
        final List<HedgeIndex> hedgeIndexList = new ArrayList<>();
        for (StrategyTrade trade : strategyTrades) {
            HedgeIndex hedgeIndex = new HedgeIndex();
            hedgeIndex.setDate(trade.getDate());
            hedgeIndex.setCategory("Yield Curve");
            hedgeIndex.setIndicator("^TNX");
            hedgeIndex.setRuleDesc("10Y - 13W spread < -50 bps (deep inversion)");
            hedgeIndex.setRisk(trade.getType().equals(Trade.TradeType.SELL.toString()) ? Boolean.TRUE : Boolean.FALSE);
            hedgeIndex.setPrice(trade.getPrice());
            hedgeIndexList.add(hedgeIndex);
        }
        return hedgeIndexList;
    }

    // In-memory cache: date millis → risk count. Loaded lazily on first risk() call.
    private volatile NavigableMap<Long, Integer> riskCache = null;

    private static final java.time.ZoneId STOCKHOLM = java.time.ZoneId.of("Europe/Stockholm");

    /**
     * Pre-loads the entire HedgeIndex table into memory so that per-bar rule
     * evaluation does not hit the database for every bar of every security.
     * Call this once before running a batch of strategies, or it is loaded
     * lazily on the first {@link #risk} call.
     */
    public synchronized void warmCache() {
        if (riskCache == null) {
            buildCache();
        }
    }

    /** Clears the cache (call after HedgeIndex data is updated). */
    public synchronized void clearCache() {
        riskCache = null;
    }

    /**
     * HedgeIndex rows are state-change <em>events</em> (one per hedge strategy
     * trade: BUY = indicator back to risk-on, SELL = indicator flipped to
     * risk-off), not daily snapshots. The score for any given day is therefore
     * the number of indicators whose most recent event on or before that day
     * left them in the risk-off state — so the cache is built by replaying all
     * events chronologically and carrying each indicator's state forward.
     */
    private void buildCache() {
        List<HedgeIndex> all = hedgeIndexRepository.findAll();
        all.sort(Comparator.comparing(HedgeIndex::getDate, Comparator.nullsFirst(Comparator.naturalOrder())));

        TreeMap<Long, Integer> cache = new TreeMap<>();
        Map<String, Boolean> stateByIndicator = new HashMap<>();
        for (HedgeIndex hi : all) {
            if (hi.getDate() == null) {
                continue;
            }
            stateByIndicator.put(hi.getIndicator(), hi.getRisk());
            cache.put(startOfDayKey(hi.getDate().toInstant()), scoreOf(stateByIndicator));
        }

        riskCache = cache;
        log.info("HedgeIndex cache warmed: {} distinct dates loaded, current score={}",
                cache.size(), cache.isEmpty() ? 0 : cache.lastEntry().getValue());
    }

    private static int scoreOf(Map<String, Boolean> stateByIndicator) {
        int score = 0;
        for (Boolean risk : stateByIndicator.values()) {
            if (Boolean.TRUE.equals(risk)) {
                score++;
            }
        }
        // Volatility cluster rule: VIX + VSTOXX both risk-off → +1 extra point
        if (Boolean.TRUE.equals(stateByIndicator.get("^VIX"))
                && Boolean.TRUE.equals(stateByIndicator.get("^V2TX"))) {
            score++;
        }
        return score;
    }

    private static long startOfDayKey(java.time.Instant instant) {
        return instant.atZone(STOCKHOLM).toLocalDate().atStartOfDay(STOCKHOLM).toInstant().toEpochMilli();
    }

    /**
     * Return risk, hence if risk < threshold go long
     * if risk > threshold, go short.
     *
     * @param indexDate
     * @return true if risk
     */
    public boolean risk(ZonedDateTime indexDate) {
        return getScore(indexDate) > riskThreshold;
    }

    /**
     * Returns the risk score in effect on the given date: the most recent
     * score on or before that day (Stockholm time). Works for any timestamp —
     * daily bar end times, 15-minute intraday bars, or {@code ZonedDateTime.now()}.
     * Higher score = more risk-off indicators in effect.
     */
    public int getScore(ZonedDateTime indexDate) {
        if (riskCache == null) {
            synchronized (this) {
                if (riskCache == null) {
                    buildCache();
                }
            }
        }
        Map.Entry<Long, Integer> entry = riskCache.floorEntry(startOfDayKey(indexDate.toInstant()));
        return entry != null ? entry.getValue() : 0;
    }

    /** Alias for {@link #getScore(ZonedDateTime)} — the score in effect on that day. */
    public int getScoreForDay(ZonedDateTime day) {
        return getScore(day);
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
