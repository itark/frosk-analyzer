package nu.itark.frosk.controller;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import nu.itark.frosk.dataset.DateManager;
import nu.itark.frosk.dataset.IndicatorValue;
import nu.itark.frosk.dataset.Trade;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DailyPrice;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.TradesRepository;
import nu.itark.frosk.service.TimeSeriesService;

@RestController
//@RequestMapping("/frosk-analyzer")
public class DataController {
	Logger logger = Logger.getLogger(DataController.class.getName());

	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;	

	@Autowired
	TradesRepository tradesRepository;		

	@Autowired
	DataSetRepository datasetRepository;		
	
	@Autowired
	TimeSeriesService timeSeriesService;	

	@Autowired
	StrategyAnalysis strategyAnalysis;

	@Autowired
	StrategyFilter strategyFilter;

	//  curl localhost:8080/actuator/health


	/**
	 * @Example  curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/MovingMomentumStrategy/COINBASE
	 *
	 * curl -XGET http://localhost:8080/frosk-analyzer/featuredStrategies/RSI2Strategy/CB
	 * 
	 * @return
	 */		
	@RequestMapping(path="/featuredStrategies/{strategy}/{dataset}", method=RequestMethod.GET)
	public List<FeaturedStrategyDTO> getFeaturedStrategies(@PathVariable("strategy") String strategy, @PathVariable("dataset") String dataset){
		logger.info("strategy="+strategy+", dataset="+dataset);
		List<FeaturedStrategyDTO> returnList = new ArrayList<>();

		DataSet datasetet = datasetRepository.findByName(dataset);
		if(Objects.isNull(datasetet)){
			logger.log(Level.WARNING, "Kunde inte hitta Dataset för :"+dataset+ " kolla ditt data.");
			throw new RuntimeException("Kunde inte hitta Dataset för :"+dataset+ " kolla ditt data.");
		}

		if ("ALL".equals(strategy)) {
			List<String> strategies = StrategiesMap.buildStrategiesMap();
			strategies.forEach(strategyName -> {
				datasetet.getSecurities().forEach(security -> {
					FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, security.getName() );
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
				if(Objects.isNull(fs)){
					logger.log(Level.WARNING, "Kunde inte hitta FeaturedStrategy för "+strategy+" och "+security.getName()+". Kolla ditt data. Kanske inte kört runStrategy");
					return;
					//throw new RuntimeException("Kunde inte hitta FeaturedStrategy för "+strategy+" och "+security.getName()+". Kolla ditt data. Kanske inte kört runStrategy");
				}
				returnList.add(getDTO(fs));
			});			
		}

		return returnList.stream().sorted(Comparator.comparing(FeaturedStrategyDTO::getName)).collect(Collectors.toList());
	//	return returnList;
	}


	private FeaturedStrategyDTO getDTO(FeaturedStrategy fs) {
		FeaturedStrategyDTO dto = new FeaturedStrategyDTO();
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

		return dto;
	}
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer/dailyPrices?security=BOL.ST
	 * 
	 * @return
	 */
	@RequestMapping(path="/dailyPrices", method=RequestMethod.GET)
	public List<DailyPrice> getDailyPrices(@RequestParam("security") String security, @RequestParam("strategy") String strategy) {
		logger.info("/dailyPrices...security=" + security);
		DailyPrice dailyPrices = null;
		List<DailyPrice> dpList = new ArrayList<DailyPrice>();
		TimeSeries timeSeries = timeSeriesService.getDataSet(security);
		for (int i = 0; i < timeSeries.getBarCount(); i++) {
			Bar bar = timeSeries.getBar(i);
			dailyPrices = new DailyPrice(bar);
			dpList.add(dailyPrices);
		}
		decorateWithTrades(dpList,security,strategy);

		return dpList;
	}

	private void decorateWithTrades(List<DailyPrice> dpList, String security, String strategy) {
		List<DailyPrice> dpListTrades = new ArrayList<>();
		for (Trade trade : getTrades(security, strategy)) {
			dpList.stream()
					.filter(dp -> dp.getDate() == trade.getDate())
					.forEach(dp -> dp.setTrade(trade.getType()));
			logger.info("trade:" + trade);
		}
	}

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/indicatorValues?security=VOLV-B.ST&strategy=RSI2Strategy
	 * 
	 * @return
	 */
	@RequestMapping(path="/indicatorValues", method=RequestMethod.GET)
	public List<IndicatorValue> getIndicatorValues(@RequestParam("security") String security, @RequestParam("strategy") String strategy){
		logger.info("/indicatorValues?security="+security+"&strategy="+strategy);
		TimeSeries timeSeries = timeSeriesService.getDataSet(security);
		strategyAnalysis.run(strategy, timeSeriesService.getSecurityId(security));
		List<IndicatorValue> indicatorsValues = strategyAnalysis.getIndicatorValues(strategy, timeSeries);

		return indicatorsValues;
	}

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/trades?security=SAND.ST&strategy=RSI2Strategy
	 * @param security
	 * @param strategy
	 * @return
	 */
	@RequestMapping(path="/trades", method=RequestMethod.GET)
	public List<Trade> getTradees(@RequestParam("security") String security, @RequestParam("strategy") String strategy){
		logger.info("/trades?security="+security+"&strategy="+strategy);
		return getTrades(security, strategy);
	}

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/longtrades
	 */
	@GetMapping(value= "/longtrades")
	public List<Trade> longTrades(){
		logger.info("/longtrades");
		return getLongTrades();
	}

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/shorttrades
	 */
	@GetMapping(value= "/shorttrades")
	public List<Trade> shortTrades(){
		logger.info("/shorttrades");
		return getShortTrades();
	}

	private List<Trade> getLongTrades() {
		return strategyFilter.getLongTradesAllStrategies();
	}

	private List<Trade> getShortTrades() {
		return strategyFilter.getShortTradesAllStrategies();
	}

	private  List<Trade> getTrades(String security, String strategy){
		return strategyFilter.getTrades(security,strategy);
	}

}