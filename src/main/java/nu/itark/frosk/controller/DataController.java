package nu.itark.frosk.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;

import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.dataset.DailyPrices;
import nu.itark.frosk.dataset.TradeView;
import nu.itark.frosk.model.Customer;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.CustomerRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.service.FeaturedStrategyService;
import nu.itark.frosk.service.TimeSeriesService;

@RestController
public class DataController {
	Logger logger = Logger.getLogger(WebController.class.getName());
	
	@Autowired
	CustomerRepository custRepository;

	@Autowired
	SecurityPriceRepository securityPriceRepository;	
	
	@Autowired
	SecurityRepository securityRepository;	
	
	@Autowired
	TimeSeriesService timeSeriesService;	

	@Autowired
	FeaturedStrategyService featuredStrategyService;	
	
	// inject via application.properties
	@Value("${welcome.message:test}")
	String message = "Hello World";	
	
	
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

		List<Security> list =securityRepository.findByName(name);
		
		securityRepository.delete(list);
		
		return "Deleted";


	}	
	
	

	/**
	 * @Example  http://localhost:8080/featuredStrategies?strategy=ALL
	 * 
	 * @param securityName
	 * @param database
	 * @return
	 */			
	@RequestMapping(path="/featuredStrategies", method=RequestMethod.GET)
	public List<FeaturedStrategyDTO> getAllFeaturedStrategies(@RequestParam("strategy") String strategy){
		logger.info("strategy="+strategy);
		//Sanity check
		if (StringUtils.isEmpty(strategy) ) {
			throw new RuntimeException("strategy not correct set!");
		}

		return featuredStrategyService.getFeaturedStrategy(strategy);

	}	
	
	/**
	 * @Example  http://localhost:8080/dailyPrices?security=WIKI/AAPL
	 * 
	 * @param securityName
	 * @param database
	 * @return
	 */
	@RequestMapping(path="/dailyPrices", method=RequestMethod.GET)
	public List<DailyPrices> getDailyPrices(@RequestParam("security") String securityName){
		logger.info("/dailyPrices...securityName="+securityName);
		try {

			TimeSeries timeSeries = timeSeriesService.getDataSet(securityName);

			DailyPrices dailyPrices = null;
			List<DailyPrices> dpList = new ArrayList<DailyPrices>();

			for (int i = 0; i < timeSeries.getBarCount(); i++) {
				Bar bar = timeSeries.getBar(i);
				dailyPrices = new DailyPrices(bar);
				dpList.add(dailyPrices);

			}

			return dpList;
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
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
		
		featuredStrategyService.getFeaturedStrategy(strategyName);
		
		
		return featuredStrategyService.getTrades(strategyName, securityName);
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
	public List<Customer> findAll2(){
		List<Customer> list = new ArrayList<Customer>();
		
		for(Customer cust : custRepository.findAll()){
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

