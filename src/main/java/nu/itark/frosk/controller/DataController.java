package nu.itark.frosk.controller;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import nu.itark.frosk.analysis.*;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.service.BarSeriesService;

@RestController
public class DataController {
    Logger logger = Logger.getLogger(DataController.class.getName());

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;


    @Autowired
    DataSetRepository datasetRepository;

    @Autowired
    BarSeriesService timeSeriesService;

    @Autowired
    StrategyAnalysis strategyAnalysis;

    @Autowired
    StrategyFilter strategyFilter;

    @Autowired
    SecurityMetaDataManager securityMetaDataManager;

    /**
     * @return
     * @Example curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/MovingMomentumStrategy/COINBASE
     * <p>
     * curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/RSI2Strategy/CB
     */
    @RequestMapping(path = "/featuredStrategies/{strategy}/{dataset}", method = RequestMethod.GET)
    public List<FeaturedStrategyDTO> getFeaturedStrategies(@PathVariable("strategy") String strategy, @PathVariable("dataset") String dataset) {
        logger.info("strategy=" + strategy + ", dataset=" + dataset);
        List<FeaturedStrategyDTO> returnList = new ArrayList<>();

        DataSet datasetet = datasetRepository.findByName(dataset);

        if (Objects.isNull(datasetet)) {
            logger.log(Level.WARNING, "Kunde inte hitta Dataset för :" + dataset + " kolla ditt data.");
            throw new RuntimeException("Kunde inte hitta Dataset för :" + dataset + " kolla ditt data.");
        }

        if ("ALL".equals(strategy)) {
            List<String> strategies = StrategiesMap.buildStrategiesMap();
            strategies.forEach(strategyName -> {
                datasetet.getSecurities().forEach(security -> {
                    FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, security.getName());
                    returnList.add(getDTO(fs));
                });
            });
        } else {
            datasetet.getSecurities().forEach(security -> {
                FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security.getName());
/*
				if (!"BTC-EUR".equals(fs.getSecurityName())) {
					logger.info("fs.getSecurityName():"+fs.getSecurityName());
					return;
				}
*/
                if (Objects.isNull(fs)) {
                    logger.log(Level.WARNING, "Kunde inte hitta FeaturedStrategy för " + strategy + " och " + security.getName() + ". Kolla ditt data. Kanske inte kört runStrategy");
                    return;
                    //throw new RuntimeException("Kunde inte hitta FeaturedStrategy för "+strategy+" och "+security.getName()+". Kolla ditt data. Kanske inte kört runStrategy");
                }
                returnList.add(getDTO(fs));
            });
        }

        return returnList.stream().sorted(Comparator.comparing(FeaturedStrategyDTO::getName)).collect(Collectors.toList());
        //	return returnList;
    }


    @RequestMapping(path = "/strategies", method = RequestMethod.GET)
    public List<String> getStrategies() {
        return StrategiesMap.buildStrategiesMap();
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/prices?security=BTC-EUR
     */
    @RequestMapping(path = "/prices", method = RequestMethod.GET)
    public List<DailyPriceDTO> getPrices(@RequestParam("security") String security) {
        logger.info("/prices...security=" + security);
        DailyPriceDTO dailyPrices = null;
        SecurityDTO frosk = null;
        List<DailyPriceDTO> dpList = new ArrayList<DailyPriceDTO>();
        BarSeries timeSeries = timeSeriesService.getDataSet(security, true);
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
        logger.info("/metadata");
        return securityMetaDataManager.getSecurityMetaData();
    }

    /**
     * @return
     * @Example http://localhost:8080/frosk-analyzer/dailyPrices?security=BTC-EUR&strategy=MovingMomentumStrategy
     */
    @Deprecated
    @RequestMapping(path = "/dailyPrices", method = RequestMethod.GET)
    public List<DailyPriceDTO> getDailyPrices(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        logger.info("/dailyPrices...security=" + security);
        DailyPriceDTO dailyPrices = null;
        List<DailyPriceDTO> dpList = new ArrayList<DailyPriceDTO>();
        BarSeries timeSeries = timeSeriesService.getDataSet(security, false);
        for (int i = 0; i < timeSeries.getBarCount(); i++) {
            Bar bar = timeSeries.getBar(i);
            dailyPrices = new DailyPriceDTO(bar);
            dpList.add(dailyPrices);
        }
        //decorateWithTrades(dpList,security,strategy);

        return dpList;
    }

    /**

     * @Example http://localhost:8080/frosk-analyzer/featuredStrategy?security=BTC-EUR&strategy=RSI2Strategy
     */
    @RequestMapping(path = "/featuredStrategy", method = RequestMethod.GET)
    public FeaturedStrategyDTO getFeaturedStrategy(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        logger.info("/featuredStrategy?security=" + security + "&strategy=" + strategy);
        final FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategy, security);
        return getDTO(featuredStrategy);
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/indicatorValues?security=VOLV-B.ST&strategy=RSI2Strategy
     */
    @Deprecated
    @RequestMapping(path = "/indicatorValues", method = RequestMethod.GET)
    public List<IndicatorValueDTO> getIndicatorValues(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
        logger.info("/indicatorValues?security=" + security + "&strategy=" + strategy);
        List<IndicatorValueDTO> indicatorValues = new ArrayList<>();
        strategyAnalysis.run(strategy, timeSeriesService.getSecurityId(security));

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
        logger.info("/trades?security=" + security + "&strategy=" + strategy);
        return getTrades(security, strategy);
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/longtrades
     */
    @GetMapping(value = "/longtrades")
    public List<TradeDTO> longTrades() {
        logger.info("/longtrades");
        return getLongTrades();
    }

    /**
     * @Example http://localhost:8080/frosk-analyzer/shorttrades
     */
    @GetMapping(value = "/shorttrades")
    public List<TradeDTO> shortTrades() {
        logger.info("/shorttrades");
        return getShortTrades();
    }

	private FeaturedStrategyDTO getDTO(FeaturedStrategy fs) {
		FeaturedStrategyDTO dto = new FeaturedStrategyDTO();
        List<IndicatorValueDTO> indicatorValues = new ArrayList<>();

		dto.setName(fs.getName());
		dto.setSecurityName(fs.getSecurityName());
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
            tradee.setPrice(trade.getPrice().longValue());
            tradee.setType(trade.getType());
            tradee.setSecurityName(trade.getFeaturedStrategy().getSecurityName());
            tradee.setStrategy(trade.getFeaturedStrategy().getName());
            trades.add(tradee);
        });
        return trades;
    }

    private List<TradeDTO> getLongTrades() {
        return strategyFilter.getLongTradesAllStrategies();
    }

    private List<TradeDTO> getShortTrades() {
        return strategyFilter.getShortTradesAllStrategies();
    }

    private List<TradeDTO> getTrades(String security, String strategy) {
        return strategyFilter.getTrades(security, strategy);
    }

}