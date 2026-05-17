package nu.itark.frosk.controller;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.HighLander;
import nu.itark.frosk.analysis.*;
import nu.itark.frosk.service.HedgeIndexService;
import nu.itark.frosk.service.PortfolioService;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.HedgeIndexRepository;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class DataController {

    @Value("${frosk.database.only:YAHOO}")
    private String databaseOnly;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    HedgeIndexRepository hedgeIndexRepository;

    @Autowired
    DataSetRepository datasetRepository;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    TradingAccountService tradingAccountService;

    @Autowired
    StrategyAnalysis strategyAnalysis;

    @Autowired
    StrategyFilter strategyFilter;

    @Autowired
    SecurityMetaDataManager securityMetaDataManager;

    @Autowired
    StrategiesMap strategiesMap;

    @Autowired
    HighLander highLander;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    IntradayBarRepository intradayBarRepository;

    @Autowired
    IntradaySignalRepository intradaySignalRepository;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    HedgeIndexService hedgeIndexService;


    /**
     * @return
     * @Example curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies
     * <p>
     * curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies
     */
    @RequestMapping(path = "/featuredStrategies", method = RequestMethod.GET)
    public List<FeaturedStrategyDTO> getFeaturedStrategies() {
        log.info("/featuredStrategies...");
        List<FeaturedStrategyDTO> featuredStrategies = strategyFilter.getFeaturedStrategies();
        log.info("/featuredStrategies, found:{}",featuredStrategies.size());
        return featuredStrategies;
    }

    @RequestMapping(path = "/topFeaturedStrategies", method = RequestMethod.GET)
    public List<FeaturedStrategyDTO> getTopFeaturedStrategies() {
        return strategyFilter.getTopFeaturedStrategies();
    }

    /**
     * @return
     * @Example curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/MovingMomentumStrategy/COINBASE
     * <p>
     * curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/RSI2Strategy/CB
     */
    @RequestMapping(path = "/featuredStrategies/{strategy}/{dataset}", method = RequestMethod.GET)
    public List<FeaturedStrategyDTO> getFeaturedStrategies(@PathVariable("strategy") String strategy, @PathVariable("dataset") String dataset) {
        log.info("strategy=" + strategy + ", dataset=" + dataset);
        List<FeaturedStrategyDTO> returnList = new ArrayList<>();
        DataSet datasetet = datasetRepository.findByName(dataset);
        if (Objects.isNull(datasetet)) {
            log.error("Kunde inte hitta Dataset för :" + dataset + " kolla ditt data.");
            throw new RuntimeException("Kunde inte hitta Dataset för :" + dataset + " kolla ditt data.");
        }

        if ("ALL".equals(strategy)) {
            List<String> strategies = strategiesMap.buildStrategiesMap();
            strategies.forEach(strategyName -> {
                datasetet.getSecurities().forEach(security -> {
                    FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, security.getName());
                    returnList.add(securityMetaDataManager.getDTO(fs, false));
                });
            });
        } else {
            datasetet.getSecurities().forEach(security -> {
                FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security.getName());
                if (Objects.isNull(fs)) {
                    log.info("Kunde inte hitta FeaturedStrategy för " + strategy + " och " + security.getName() + ". Kolla ditt data. Kanske inte kört runStrategy");
                    return;
                }
                returnList.add(securityMetaDataManager.getDTO(fs, false));
            });
        }

        return returnList.stream().sorted(Comparator.comparing(FeaturedStrategyDTO::getName)).collect(Collectors.toList());
        //	return returnList;
    }


    @RequestMapping(path = "/strategies", method = RequestMethod.GET)
    public List<String> getStrategies() {
        log.info("/strategies");
        return strategiesMap.buildStrategiesMap();
    }


    @RequestMapping(path = "/actions", method = RequestMethod.GET)
    public List<String> getActions() {
        log.info("/actions");
        return Arrays.stream(HighLander.ACTION.values())
                .map(Enum::toString) // or .map(Object::toString)
                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/runAction", method = RequestMethod.GET)
    public void runAction(@RequestParam("action") String action, @RequestParam("security") String securityId, @RequestParam("strategy") String strategy) {
        log.info("/runAction, action:{}, securityId:{}",action,securityId);
        if (action.equals("undefined")) return;

        if (HighLander.ACTION.valueOf(action) == HighLander.ACTION.LOAD_DATA) {
            log.info("action:{}",action);
            highLander.addSecurityPriceFromDatabase(Long.valueOf(securityId), Database.YAHOO);
            highLander.updateSecurityMetaData(Long.valueOf(securityId));
        }
        if (HighLander.ACTION.valueOf(action) == HighLander.ACTION.RUN_STRATEGY) {
            log.info("action:{}",action);
            highLander.runStrategy(strategy,Long.valueOf(securityId));
        }
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/prices?security=BTC-EUR
     */
    @RequestMapping(path = "/prices", method = RequestMethod.GET)
    public List<DailyPriceDTO> getPrices(@RequestParam("security") String security) {
        log.info("/prices for security:{}",security);
        DailyPriceDTO dailyPrices = null;
        List<DailyPriceDTO> dpList = new ArrayList<DailyPriceDTO>();

        BarSeries timeSeries = barSeriesService.getDataSet(security, false, false);
        for (int i = 0; i < timeSeries.getBarCount(); i++) {
            Bar bar = timeSeries.getBar(i);
            dailyPrices = new DailyPriceDTO(bar);
            dpList.add(dailyPrices);
        }

        return dpList;
    }

    /**
     * @Example http://localhost:8080/intradayPrices?security=VOLV-B.ST
     */
    @GetMapping(path = "/intradayPrices")
    public List<DailyPriceDTO> getIntradayPrices(@RequestParam("security") String security) {
        log.info("/intradayPrices for security:{}", security);
        Security sec = securityRepository.findByName(security);
        if (sec == null) {
            log.warn("Security not found: {}", security);
            return Collections.emptyList();
        }
        List<IntradayBar> bars = intradayBarRepository.findBySecurityIdOrderByBarTimestampAsc(sec.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return bars.stream()
                .map(ib -> {
                    DailyPriceDTO dto = new DailyPriceDTO();
                    dto.setTime(Instant.ofEpochSecond(ib.getBarTimestamp())
                            .atZone(ZoneId.of("Europe/Stockholm")).format(fmt));
                    dto.setOpen(ib.getOpen().doubleValue());
                    dto.setHigh(ib.getHigh().doubleValue());
                    dto.setLow(ib.getLow().doubleValue());
                    dto.setClose(ib.getClose().doubleValue());
                    dto.setValue(ib.getVolume() != null ? ib.getVolume() : 0L);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/metadata
     */
    @RequestMapping(path = "/metadata", method = RequestMethod.GET)
    public List<SecurityDTO> getMetaData() {
        log.info("/metadata");
        return securityMetaDataManager.getSecurityMetaData(databaseOnly);
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/security?name=ABB.ST
     */
    @RequestMapping(path = "/security", method = RequestMethod.GET)
    public SecurityDTO getSecurity(@RequestParam("name") String name) {
        return securityMetaDataManager.getSecurity(name);
    }

    /**
     *
     * @Example http://localhost:8080/frosk-analyzer/topStrategies
     */
    @RequestMapping(path = "/topStrategies", method = RequestMethod.GET)
    public List<TopStrategyDTO> getTopStrategies() {
        log.info("/topStrategies");
        return strategyFilter.findBestPerformingStrategies().stream()
                .map(dto -> TopStrategyDTO.builder()
                        .name(dto.getName().replace("Strategy",""))
                        .totalProfit(dto.getTotalProfit())
                        .sqn(dto.getSqn())
                        .sqnRaw(dto.getSqnRaw())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     *
     * @Example http://localhost:8080/frosk-analyzer/riskCumulative
     */
    @RequestMapping(path = "/riskCumulative", method = RequestMethod.GET)
    public List<RiskCumulativeDTO> getRiskCumulative() {
        log.info("/riskCumulative");
        return hedgeIndexRepository.summarizeCumulativeRiskPerDate().stream()
                .map(dto -> RiskCumulativeDTO.builder()
                        .dayDate(dto.getDayDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .riskyCount(dto.getRiskyCount())
                        .nonRiskyCount(dto.getNonRiskyCount())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/dailyPrices?security=BTC-EUR&strategy=MovingMomentumStrategy
     */
    @Deprecated
    @RequestMapping(path = "/dailyPrices", method = RequestMethod.GET)
    public List<DailyPriceDTO> getDailyPrices(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        log.info("/dailyPrices for security=" + security);
        DailyPriceDTO dailyPrices = null;
        List<DailyPriceDTO> dpList = new ArrayList<DailyPriceDTO>();

        BarSeries timeSeries = barSeriesService.getDataSet(security, false, false);
        for (int i = 0; i < timeSeries.getBarCount(); i++) {
            Bar bar = timeSeries.getBar(i);
            dailyPrices = new DailyPriceDTO(bar);
            dpList.add(dailyPrices);
        }
        return dpList;
    }

    /**

     * @Example http://localhost:8080/frosk-analyzer/featuredStrategy?security=BTC-EUR&strategy=RSI2Strategy
     */
    @RequestMapping(path = "/featuredStrategy", method = RequestMethod.GET)
    public FeaturedStrategyDTO getFeaturedStrategy(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        log.info("/featuredStrategy?security=" + security + "&strategy=" + strategy);
        final FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategy, security);
        return securityMetaDataManager.getDTO(featuredStrategy, true);
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/indicatorValues?security=VOLV-B.ST&strategy=RSI2Strategy
     */
    @Deprecated
    @RequestMapping(path = "/indicatorValues", method = RequestMethod.GET)
    public List<IndicatorValueDTO> getIndicatorValues(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        log.info("/indicatorValues?security=" + security + "&strategy=" + strategy);
        List<IndicatorValueDTO> indicatorValues = new ArrayList<>();
        strategyAnalysis.run(strategy, barSeriesService.getSecurityId(security));
        final FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategy, security);
        featuredStrategy.getIndicatorValues().forEach(siv -> {
            indicatorValues.add(new IndicatorValueDTO(siv.getDate(),siv.getValue(), siv.getIndicator()));
        });
        return indicatorValues;
    }

    /**
     * @param security
     * @param strategy
     * @return
     * @Example http://localhost:8080/frosk-analyzer/trades?security=SAND.ST&strategy=RSI2Strategy
     */
    @RequestMapping(path = "/trades", method = RequestMethod.GET)
    public List<TradeDTO> getTradees(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        log.info("/trades?security=" + security + "&strategy=" + strategy);
        return getTrades(security, strategy);
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/longtrades
     */
    @GetMapping(value = "/longtrades")
    public List<TradeDTO> longTrades() {
        log.info("/longtrades");
        return getLongTrades();
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/shorttrades
     */
    @GetMapping(value = "/shorttrades")
    public List<TradeDTO> shortTrades() {
        log.info("/shorttrades");
        return getShortTrades();
    }

    @GetMapping(value = "/smartSignals")
    public List<OpenFeaturedStrategyDTO> openSmartSignals(@RequestParam("open") String open) {
        log.info("/smartSignals?open={}",open);
        return getSmartSignals(Boolean.valueOf(open));
    }

    @GetMapping(value = "/openSignals")
    public List<OpenFeaturedStrategyDTO> openSignals() {
        log.info("/openSignals");
        return getOpenSignals();
    }

    /**
     * Build a new portfolio snapshot from currently open FeaturedStrategy positions.
     * @Example POST http://localhost:8080/portfolio/build
     */
    @PostMapping(value = "/portfolio/build")
    public PortfolioDTO buildPortfolio() {
        log.info("POST /portfolio/build");
        portfolioService.build();
        return portfolioService.getCurrent();
    }

    /**
     * Get the most recent portfolio snapshot with all positions.
     * @Example GET http://localhost:8080/portfolio
     */
    @GetMapping(value = "/portfolio")
    public PortfolioDTO getPortfolio() {
        log.info("GET /portfolio");
        return portfolioService.getCurrent();
    }

    /**
     * Get all historical portfolio snapshots (header only, no positions).
     * @Example GET http://localhost:8080/portfolio/history
     */
    @GetMapping(value = "/portfolio/history")
    public List<PortfolioDTO> getPortfolioHistory() {
        log.info("GET /portfolio/history");
        return portfolioService.getHistory();
    }

    /**
     * Get a specific historical portfolio snapshot with full positions.
     * @Example GET http://localhost:8080/portfolio/42
     */
    @GetMapping(value = "/portfolio/{id}")
    public PortfolioDTO getPortfolioById(@PathVariable("id") Long id) {
        log.info("GET /portfolio/{}", id);
        return portfolioService.getById(id);
    }

    /**
     * Dagstrategin next-morning watchlist.
     * Returns OMXS30 stocks with an open DailyBreakout or DailyOversoldBounce signal,
     * ranked by SQN descending (highest quality signal first).
     *
     * @Example GET http://localhost:8080/dagstrategin/watchlist
     */
    @GetMapping(value = "/dagstrategin/watchlist")
    public List<FeaturedStrategyDTO> getDagstrateginWatchlist() {
        log.info("GET /dagstrategin/watchlist");
        List<nu.itark.frosk.model.FeaturedStrategy> candidates =
                featuredStrategyRepository.findByNameInAndOpenOrderBySqnDesc(
                        List.of("DailyBreakoutStrategy", "DailyOversoldBounceStrategy"),
                        Boolean.TRUE);
        log.info("Dagstrategin watchlist: {} open signals", candidates.size());
        return candidates.stream()
                .map(fs -> securityMetaDataManager.getDTO(fs, false))
                .collect(Collectors.toList());
    }

    /**
     * @Example GET http://localhost:8080/intraday/signals
     */
    @GetMapping(value = "/intraday/signals")
    public List<IntradaySignalDTO> getIntradaySignals() {
        log.info("GET /intraday/signals");
        return intradaySignalRepository.findTop20ByOrderBySignalTimestampDesc().stream()
                .map(s -> IntradaySignalDTO.builder()
                        .ticker(s.getTicker())
                        .signalTime(Instant.ofEpochSecond(s.getSignalTimestamp())
                                .atZone(ZoneId.of("Europe/Stockholm"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        .signalType(s.getSignalType())
                        .closePrice(s.getClosePrice())
                        .ema9(s.getEma9())
                        .ema21(s.getEma21())
                        .rsi7(s.getRsi7())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * @Example GET http://localhost:8080/intraday/signals/today
     */
    @GetMapping(value = "/intraday/signals/today")
    public List<IntradaySignalDTO> getIntradaySignalsToday() {
        log.info("GET /intraday/signals/today");
        long startOfDay = LocalDate.now(ZoneId.of("Europe/Stockholm"))
                .atStartOfDay(ZoneId.of("Europe/Stockholm"))
                .toEpochSecond();
        return intradaySignalRepository.findTop20ByOrderBySignalTimestampDesc().stream()
                .filter(s -> s.getSignalTimestamp() >= startOfDay && "BUY".equals(s.getSignalType()))
                .map(s -> IntradaySignalDTO.builder()
                        .ticker(s.getTicker())
                        .signalTime(Instant.ofEpochSecond(s.getSignalTimestamp())
                                .atZone(ZoneId.of("Europe/Stockholm"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        .signalType(s.getSignalType())
                        .closePrice(s.getClosePrice())
                        .ema9(s.getEma9())
                        .ema21(s.getEma21())
                        .rsi7(s.getRsi7())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * @Example GET http://localhost:8080/hedgeindex/score
     */
    @GetMapping(value = "/hedgeindex/score")
    public HedgeIndexScoreDTO getHedgeIndexScore() {
        log.info("GET /hedgeindex/score");
        int score = hedgeIndexService.getScore(ZonedDateTime.now());
        return HedgeIndexScoreDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .score(score)
                .regime(regimeLabel(score))
                .build();
    }

    /**
     * @Example GET http://localhost:8080/hedgeindex/history?days=30
     */
    @GetMapping(value = "/hedgeindex/history")
    public List<HedgeIndexScoreDTO> getHedgeIndexHistory(@RequestParam(value = "days", defaultValue = "30") int days) {
        log.info("GET /hedgeindex/history?days={}", days);
        LocalDate cutoff = LocalDate.now().minusDays(days);
        return hedgeIndexRepository.summarizeCumulativeRiskPerDate().stream()
                .filter(p -> p.getDayDate() != null && !p.getDayDate().isBefore(cutoff))
                .map(p -> HedgeIndexScoreDTO.builder()
                        .date(p.getDayDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .score(p.getRiskyCount().intValue())
                        .regime(regimeLabel(p.getRiskyCount().intValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private String regimeLabel(int score) {
        if (score <= 3) return "Strong Risk-On";
        if (score <= 7) return "Cautious / Transition";
        if (score <= 11) return "Neutral / Defensive";
        return "Strong Risk-Off";
    }

    /**
     * @Example POST http://localhost:8080/intraday/run
     */
    @PostMapping(value = "/intraday/run")
    public String triggerIntradayRun() {
        log.info("POST /intraday/run — manual trigger");
        highLander.syncTier0();
        return "IntradayStrategyRunner completed";
    }

    /**
     * @Example GET http://localhost:8080/intradayOpenPositions
     */
    @GetMapping(value = "/intradayOpenPositions")
    public List<IntradayOpenPositionDTO> getIntradayOpenPositions() {
        log.info("GET /intradayOpenPositions");
        List<FeaturedStrategy> openPositions = featuredStrategyRepository
                .findByNameAndOpenOrderBySqnDesc("OMX30IntradayMomentumStrategy", true);

        return openPositions.stream().map(fs -> {
            Security security = securityRepository.findByName(fs.getSecurityName());
            BigDecimal entryPrice = null;
            String entryTime = "";
            if (fs.getStrategyTrades() != null && !fs.getStrategyTrades().isEmpty()) {
                StrategyTrade lastBuy = fs.getStrategyTrades().stream()
                        .filter(t -> "BUY".equals(t.getType()))
                        .max(Comparator.comparing(t -> t.getDate().getTime()))
                        .orElse(null);
                if (lastBuy != null) {
                    entryPrice = lastBuy.getPrice();
                    entryTime = lastBuy.getDate().toInstant()
                            .atZone(ZoneId.of("Europe/Stockholm"))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
            }
            BigDecimal currentPrice = null;
            if (security != null) {
                IntradayBar latestBar = intradayBarRepository
                        .findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
                if (latestBar != null) {
                    currentPrice = latestBar.getClose();
                }
            }
            BigDecimal unrealizedPnl = null;
            if (entryPrice != null && currentPrice != null && entryPrice.compareTo(BigDecimal.ZERO) != 0) {
                unrealizedPnl = currentPrice.subtract(entryPrice)
                        .divide(entryPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            return IntradayOpenPositionDTO.builder()
                    .securityName(fs.getSecurityName())
                    .entryPrice(entryPrice)
                    .entryTime(entryTime)
                    .currentPrice(currentPrice)
                    .unrealizedPnl(unrealizedPnl)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * @Example GET http://localhost:8080/intradayTodaySignals
     */
    @GetMapping(value = "/intradayTodaySignals")
    public List<IntradayTodaySignalDTO> getIntradayTodaySignals() {
        log.info("GET /intradayTodaySignals");
        long startOfDay = LocalDate.now(ZoneId.of("Europe/Stockholm"))
                .atStartOfDay(ZoneId.of("Europe/Stockholm"))
                .toEpochSecond();

        List<FeaturedStrategy> allMomentum = featuredStrategyRepository
                .findByName("OMX30IntradayMomentumStrategy");

        List<IntradayTodaySignalDTO> signals = new ArrayList<>();
        Date startOfDayDate = Date.from(Instant.ofEpochSecond(startOfDay));
        for (FeaturedStrategy fs : allMomentum) {
            if (fs.getStrategyTrades() == null) continue;
            fs.getStrategyTrades().stream()
                    .filter(t -> t.getDate().after(startOfDayDate) || t.getDate().equals(startOfDayDate))
                    .forEach(t -> signals.add(IntradayTodaySignalDTO.builder()
                            .securityName(fs.getSecurityName())
                            .type(t.getType())
                            .price(t.getPrice())
                            .date(t.getDate().toInstant()
                                    .atZone(ZoneId.of("Europe/Stockholm"))
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                            .build()));
        }
        signals.sort(Comparator.comparing(IntradayTodaySignalDTO::getDate).reversed());
        return signals;
    }

    @GetMapping(value = "/tradingAccounts")
    public List<TotalTradingDTO> tradingAccounts() {
        log.info("/tradingAccounts");
        return getTradingAccounts();
    }

    private List<TotalTradingDTO> getTradingAccounts() {
       return tradingAccountService.getTradingAccounts();
    }

    private List<TradeDTO> getLongTrades() {
        return strategyFilter.getLongTradesAllStrategies();
    }

    private List<TradeDTO> getShortTrades() {
        return strategyFilter.getShortTradesAllStrategies();
    }

    private List<OpenFeaturedStrategyDTO> getSmartSignals(Boolean open) {
        return strategyFilter.getSmartSignals(open);
    }

    private List<OpenFeaturedStrategyDTO> getOpenSignals() {
        return strategyFilter.getOpenSignals();
    }

    private List<TradeDTO> getTrades(String security, String strategy) {
        return strategyFilter.getTrades(security, strategy);
    }

}