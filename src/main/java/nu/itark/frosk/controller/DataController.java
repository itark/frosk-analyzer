package nu.itark.frosk.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.analysis.ChartValueDTO;
import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DailyPrices;
import nu.itark.frosk.dataset.IndicatorValues;
import nu.itark.frosk.dataset.TradeView;
import nu.itark.frosk.model.Customer;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.CustomerRepository;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.ChartValuesService;
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
	SecurityRepository securityRepository;	

	@Autowired
	DataSetRepository datasetRepository;		
	
	
	@Autowired
	TimeSeriesService timeSeriesService;	

	@Autowired
	ChartValuesService chartValuesService;		
	
	@Autowired
	StrategyAnalysis strategyAnalysis;	
	
	//TODO refactor
	Map<String, List<TradeView>> tradesList = new HashMap<String, List<TradeView>>();
	
	//TODO refactor
	Map<String, List<IndicatorValues>> indicatorValuesList = new HashMap<String,  List<IndicatorValues>>();
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer/featuredStrategies?strategy=RSI2Strategy
	 * 
	 * @param securityName
	 * @param database
	 * @return
	 */			
	@Deprecated
	@RequestMapping(path="/featuredStrategiesOBS", method=RequestMethod.GET)
	public List<FeaturedStrategyDTO> getFeaturedStrategiesOBS(@RequestParam("strategy") String strategy){
		logger.info("strategy="+strategy);
		if (StringUtils.isEmpty(strategy) ) {
			throw new RuntimeException("strategy not correct set!");
		}
		if ("ALL".equals(strategy)) {
			strategy = null;
		}

		List<FeaturedStrategyDTO> strategyList = strategyAnalysis.run(strategy, null);
		strategyList.forEach(dto -> {
			tradesList.put(dto.getSecurityName(), dto.getTrades());
			logger.info("Security:"+dto.getSecurityName()+" has "+dto.getIndicatorValues()+" values");
			indicatorValuesList.put(dto.getSecurityName(), dto.getIndicatorValues());
		});
		
		List<FeaturedStrategyDTO> strategyOrderedByProfitList = 
			strategyList
				.stream()
				.sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
		
		return strategyOrderedByProfitList;

	}	

	/**
	 * @Example  http://localhost:8080/frosk-analyzer/featuredStrategies?strategy=RSI2Strategy&dataset=OSCAR
	 * 
	 * @param securityName
	 * @param database
	 * @return
	 */		
	//TODO impl. dataset
	@RequestMapping(path="/featuredStrategies", method=RequestMethod.GET)
	public Iterable<FeaturedStrategy> getFeaturedStrategies(@RequestParam("strategy") String strategy, @RequestParam("dataset") String dataset){
		logger.info("strategy="+strategy+", dataset="+dataset);
		Iterable<FeaturedStrategy> list;
		List<FeaturedStrategy> returnList = new ArrayList<>();

		if (StringUtils.isEmpty(strategy) ) {
			throw new RuntimeException("strategy not correct set!");
		}
		if ("ALL".equals(strategy)) {
			list = featuredStrategyRepository.findAll();
		} else {
	
			Iterable<FeaturedStrategy> fsList = featuredStrategyRepository.findByNameOrderByTotalProfitDesc(strategy);	
			
			fsList.forEach(fs -> {
				Security security = securityRepository.findByName(fs.getSecurityName());
//				DataSet ds = datasetRepository.findBySecurityId(security.getId());
				DataSet ds = datasetRepository.findByName(dataset);
				ds.getSecurities().contains(security);

				if (ds.getSecurities().contains(security)){
					returnList.add(fs);
				}
			});
		
		}
		
		return returnList;

	}	
	
	
	
	/**
	 * @Example  http://localhost:8080/dailyPrices?security=BOL.ST
	 * 
	 * @param securityName
	 * @param database
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
	 * @Example  http://localhost:8080/dailyPrices?security=BOL.ST&strategy=RSI2Strategy
	 * 
	 * @param securityName
	 * @param database
	 * @return List<ChartValueDTO> to present in UI.
	 */
	@RequestMapping(path="/chartValues", method=RequestMethod.GET)
	public List<ChartValueDTO> getChartValues(@RequestParam("security") String security,  @RequestParam("strategy") String strategy) {
		logger.info("/chartValues...security=" + security);
		
		List<ChartValueDTO> chartValues = chartValuesService.getChartValues(strategy, security);
		
		return chartValues;

	}
	
	
	
	/**
	 * @Example  http://localhost:8080/frosk-analyzer/indicatorValues?security=SAND.ST
	 * 
	 * @param securityName
	 * @param database
	 * @return
	 */
	@RequestMapping(path="/indicatorValues", method=RequestMethod.GET)
	public List<IndicatorValues> getIndicatorValues(@RequestParam("security") String securityName){
		logger.info("/indicatorValues...securityName="+securityName);
		logger.info("/indicatorValuesList.size=="+indicatorValuesList.size());

		return indicatorValuesList.get(securityName);
		
	}	
	
	
	/**
	 * @Example  http://localhost:8080/trades?security=WIKI/AAPL&strategy=RSI2Strategy
	 * @param securityName
	 * @param strategyName
	 * @return
	 */
	@RequestMapping(path="/trades", method=RequestMethod.GET)
	public List<TradeView> getTrades(@RequestParam("security") String securityName, @RequestParam("strategy") String strategyName){
		logger.info("/trades...securityName="+securityName+"strategyName="+strategyName);	
		
		return tradesList.get(securityName);

	}	
	

	
	
	
	
	
	//Below demo stuff on JPA

	/**
	 * @Example  http://localhost:8080/frosk-analyzer-0.0.1/dummy?id=ALL
	 * 
	 * @param securityName
	 * @param database
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
	 * @param securityName
	 * @param database
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
	 * @param securityName
	 * @param database
	 * @return
	 */			
	@RequestMapping(path="/dummy_delete", method=RequestMethod.GET)
	public String doDummyDelete(@RequestParam("name") String name){
		logger.info("delete name="+name);

		Security sec =securityRepository.findByName(name);
		
		securityRepository.delete(sec);
		
		return "Deleted";


	}	
	
	
	@RequestMapping("/save")
	public String process(){
		// save a single Customer
		custRepository.save(new Customer("Jack", "Smith"));
		// save a list of Customers
		custRepository.save(Arrays.asList(new Customer("Adam", "Johnson"), new Customer("Kim", "Smith"),
										new Customer("David", "Williams"), new Customer("Peter", "Davis")));
		
		return "Done";
	}
	
	@RequestMapping("/findall")
	public String findAll(){
		String result = "";
		for(Customer cust : custRepository.findAll()){
			result += cust.toString() + "<br>";
		}
		
		return result;
	}
	
	@RequestMapping("/findall2")
	public List<Customer> findAll2() {
		List<Customer> list = new ArrayList<Customer>();
		for (Customer cust : custRepository.findAll()) {
			list.add(cust);
		}
		return list;
	}	
	
	@RequestMapping("/findbyid")
	public String findById(@RequestParam("id") long id){
		String result = "";
		result = custRepository.findOne(id).toString();
		return result;
	}
	
	@RequestMapping("/findbylastname")
	public String fetchDataByLastName(@RequestParam("lastname") String lastName){
		String result = "";
		for(Customer cust: custRepository.findByLastName(lastName)){
			result += cust.toString() + "<br>"; 
		}
		
		return result;
	}
}