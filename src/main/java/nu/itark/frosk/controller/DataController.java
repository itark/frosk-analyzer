package nu.itark.frosk.controller;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.HighLander;
import nu.itark.frosk.analysis.*;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

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

    @RequestMapping(path = "/runSelectedAction", method = RequestMethod.GET)
    public void getRunSelectedAction(@RequestParam("action") String action, @RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        log.info("/runSelectedAction, action:{}, security:{}, strategy:{}",action,security,strategy);
        if (action.equals("undefined")) return;  //TODO fix

        if (HighLander.ACTION.valueOf(action) == HighLander.ACTION.LOAD_DATA) {
            log.info("action:{}",action);
            highLander.addSecurityPriceFromCoinbase(security);
        }
        if (HighLander.ACTION.valueOf(action) == HighLander.ACTION.RUN_STRATEGY) {
            log.info("action:{}",action);
            highLander.runStrategy(strategy,security);
        }
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/prices?security=BTC-EUR
     */
    @RequestMapping(path = "/prices", method = RequestMethod.GET)
    public List<DailyPriceDTO> getPrices(@RequestParam("security") String security) {
        log.info("/prices for security=" + security);
        DailyPriceDTO dailyPrices = null;
        SecurityDTO frosk = null;
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
     * @return
     * @Example http://localhost:8080/frosk-analyzer/metadata
     */
    @RequestMapping(path = "/metadata", method = RequestMethod.GET)
    public List<SecurityDTO> getMetaData() {
        log.info("/metadata");
        return securityMetaDataManager.getSecurityMetaData(databaseOnly);
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