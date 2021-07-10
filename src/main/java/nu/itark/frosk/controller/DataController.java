package nu.itark.frosk.controller;

import java.util.*;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ta4j.core.Bar;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.TradingRecord;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DailyPrices;
import nu.itark.frosk.model.Customer;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.CustomerRepository;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.repo.StrategyIndicatorValueRepository;
import nu.itark.frosk.repo.TradesRepository;
import nu.itark.frosk.service.TimeSeriesService;

@RestController
public class DataController {
	Logger logger = Logger.getLogger(DataController.class.getName());
	
	@Autowired
	CustomerRepository custRepository;

	@Autowired
	SecurityPriceRepository securityPriceRepository;
	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;	

	@Autowired
	TradesRepository tradesRepository;		

	@Autowired
	StrategyIndicatorValueRepository stratIndicatorValueRepository;		
	
	@Autowired
	SecurityRepository securityRepository;	

	@Autowired
	DataSetRepository datasetRepository;		
	
	
	@Autowired
	TimeSeriesService timeSeriesService;	

	@Autowired
	StrategyAnalysis strategyAnalysis;	
	
//	//TODO refactor
//	Map<String, List<TradeView>> tradesList = new HashMap<String, List<TradeView>>();
//	
//	//TODO refactor
//	Map<String, List<IndicatorValues>> indicatorValuesList = new HashMap<String,  List<IndicatorValues>>();
	
//	/**
//	 * @Example  http://localhost:8080/frosk-analyzer/featuredStrategies?strategy=RSI2Strategy
//	 * 
//	 * @param securityName
//	 * @param database
//	 * @return
//	 */			
//	@Deprecated
//	@RequestMapping(path="/featuredStrategiesOBS", method=RequestMethod.GET)
//	public List<FeaturedStrategyDTO> getFeaturedStrategiesOBS(@RequestParam("strategy") String strategy){
//		logger.info("strategy="+strategy);
//		if (StringUtils.isEmpty(strategy) ) {
//			throw new RuntimeException("strategy not correct set!");
//		}
//		if ("ALL".equals(strategy)) {
//			strategy = null;
//		}
//
//		List<FeaturedStrategyDTO> strategyList = strategyAnalysis.run(strategy, null);
//		strategyList.forEach(dto -> {
//			tradesList.put(dto.getSecurityName(), dto.getTrades());
//			logger.info("Security:"+dto.getSecurityName()+" has "+dto.getIndicatorValues()+" values");
//			indicatorValuesList.put(dto.getSecurityName(), dto.getIndicatorValues());
//		});
//		
//		List<FeaturedStrategyDTO> strategyOrderedByProfitList = 
//			strategyList
//				.stream()
//				.sorted(Comparator.reverseOrder())
//				.collect(Collectors.toList());
//		
//		return strategyOrderedByProfitList;
//
//	}	

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/featuredStrategies?strategy=RSI2Strategy&dataset=OSCAR
	 * 
	 * @return
	 */		
	@RequestMapping(path="/featuredStrategies", method=RequestMethod.GET)
	public List<FeaturedStrategyDTO> getFeaturedStrategies(@RequestParam("strategy") String strategy, @RequestParam("dataset") String datasetName){
		logger.info("strategy="+strategy+", dataset="+datasetName);
		List<FeaturedStrategyDTO> returnList = new ArrayList<>();

		DataSet dataset = datasetRepository.findByName(datasetName);

		if ("ALL".equals(strategy)) {
			List<String> strategies = StrategiesMap.buildStrategiesMap();
			strategies.forEach(strategyName -> {
				dataset.getSecurities().forEach(security -> {
					FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, security.getName() );
					returnList.add(getDTO(fs));
				});					
			});			
		} else {
			dataset.getSecurities().forEach(security -> {
				FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security.getName() );
				returnList.add(getDTO(fs));
			});			
		}

		return returnList;
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
	 * @Example  http://localhost:8080/dailyPrices?security=BOL.ST
	 * 
	 * @return
	 */
	@RequestMapping(path="/dailyPrices", method=RequestMethod.GET)
	public List<DailyPrices> getDailyPrices(@RequestParam("security") String securityName) {
		logger.info("/dailyPrices...securityName=" + securityName);

		TimeSeries timeSeries = timeSeriesService.getDataSet(securityName);

		DailyPrices dailyPrices = null;
		List<DailyPrices> dpList = new ArrayList<DailyPrices>();

		for (int i = 0; i < timeSeries.getBarCount(); i++) {
			Bar bar = timeSeries.getBar(i);
			dailyPrices = new DailyPrices(bar);
			dpList.add(dailyPrices);

		}

		return dpList;

	}


	/**
	 * @Example  http://localhost:8080/frosk-analyzer/indicatorValues?security=VOLV-B.ST&strategy=RSI2Strategy
	 * 
	 * @return
	 */
	@RequestMapping(path="/indicatorValues", method=RequestMethod.GET)
	public List<StrategyIndicatorValue> getIndicatorValues(@RequestParam("security") String security, @RequestParam("strategy") String strategy){
		logger.info("/indicatorValues...security="+security+"strategy="+strategy);	
//		List<StrategyIndicatorValue> indicatorValueList = new ArrayList<StrategyIndicatorValue>();

		
		TimeSeries timeSeries = timeSeriesService.getDataSet(security);

//		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
//		TradingRecord tradingRecord = seriesManager.run(strategy);
//		trades = tradingRecord.getTrades();		
		
//		Strategy strategyS = strategyAnalysis.getStrategyToRun(strategy, timeSeries, null);
		
	
		strategyAnalysis.run(strategy, timeSeriesService.getSecurityId(security));
		List<StrategyIndicatorValue> indicatorsValues = strategyAnalysis.getIndicatorValues(strategy, timeSeries);
		
		
		
		return indicatorsValues;
		
//		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security );
//
//		stratIndicatorValueRepository.findByFeaturedStrategyOrderByDate(fs).forEach(iv -> {
//			iv.setFeaturedStrategy(null); //to be able to use entity, avoid recursion
//			indicatorValueList.add(iv);
//		});		
		
//		return indicatorValueList;
		
	}	
	
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer/trades?security=SAND.ST&strategy=RSI2Strategy
	 * @param security
	 * @param strategy
	 * @return
	 */
	@RequestMapping(path="/trades", method=RequestMethod.GET)
	public List<StrategyTrade> getTrades(@RequestParam("security") String security, @RequestParam("strategy") String strategy){
		logger.info("/trades...security="+security+"strategy="+strategy);	
		List<StrategyTrade> trades = new ArrayList<StrategyTrade>();
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategy, security );

		tradesRepository.findByFeaturedStrategy(fs).forEach(trade -> {
			trade.setFeaturedStrategy(null); //to be able to use entity, avooid recursion
			trades.add(trade);
		});
		
		return trades;

	}	
	
	//Below demo stuff on JPA

	/**
	 * @Example  http://localhost:8080/frosk-analyzer-0.0.1/dummy?id=ALL
	 * 
	 * @return
	 */			
	@RequestMapping(path="/dummy", method=RequestMethod.GET)
	public List<Security> doDummyRead(@RequestParam("id") String id){
		logger.info("id="+id);

		List<Security> list = (List<Security>) securityRepository.findAll();
		
		return list;


	}	
	
	
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer-0.0.1/dummy_insert?name=ALL
	 * 
	 * @return
	 */			
	@RequestMapping(path="/dummy_insert", method=RequestMethod.GET)
	public String doDummyInserty(@RequestParam("name") String name){
		logger.info("create name="+name);

		
		Security security = new Security(name, "manuell", "WTF");
		
		securityRepository.save(security);
		
		return "Doone";


	}		
	
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer-0.0.1/dummy_delete?name=ALL
	 * 
	 * @return
	 */			
	@RequestMapping(path="/dummy_delete", method=RequestMethod.GET)
	public String doDummyDelete(@RequestParam("name") String name){
		logger.info("delete name="+name);

		Security sec =securityRepository.findByName(name);
		
		securityRepository.delete(sec);
		
		return "Deleted";


	}	
	


}