package nu.itark.frosk.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import nu.itark.frosk.HighLander;
import nu.itark.frosk.analysis.StrategyAnalysis;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;

@Controller
public class WebController {
	Logger logger = Logger.getLogger(WebController.class.getName());

//	@Autowired
//	BITFINEXDataManager bitfinexManager;	
	
	@Autowired
	DataManager dataManager;		

	@Autowired
	StrategyAnalysis strategyAnalysis;		
	
	@Autowired
	HighLander highLander;

	@RequestMapping("/")
	public String welcome2(Map<String, Object> model) {
		return "index";	
	
	}	
	
	@RequestMapping("/strategies")
	public String strategies(Map<String, Object> model) {
		return "strategies";	
	}		
	
	@RequestMapping("/rnn")
	public String rnn(Map<String, Object> model) {
		return "rnn";	
	}	

	@RequestMapping("/rsi")
	public String rsi(Map<String, Object> model) {
		return "rsi";	
	}	
	
	@RequestMapping("/rsi_lab")
	public String rsi_lab(Map<String, Object> model) {
		return "rsi_lab";	
	}		
	
	@RequestMapping("/ma")
	public String ma(Map<String, Object> model) {
		return "ma";	
	}	
	
	@RequestMapping("/all")
	public String all(Map<String, Object> model) {
		return "all";	
	}		

	@RequestMapping("/websocket")
	public String websocket(Map<String, Object> model) {
		return "websocket";	
	}		
	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/highlander
	 */
	@RequestMapping(value="highlander", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String runHighlander(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "highlander");
		
		highLander.runInstall();
		
		return "Highlander executed";	
	}
	
	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/initDatabase
	 */
	@RequestMapping(value="initDatabase", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String initDatabase(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "initDatabases");
		dataManager.addDatasetSecuritiesIntoDatabase();
		
		return "Securities inserted";	
	}
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/fill
	 */
	@RequestMapping(value="fill", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String fill(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "fill , now YAHOO only");
		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);
		
		return "Security prices inserted";	
	}	

	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/run
	 */
	@RequestMapping(value="run", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String run(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "run , now YAHOO only");
	
		try {
			strategyAnalysis.run(null, null);
		} catch (DataIntegrityViolationException e) {
			logger.severe("Error running StrategyAnalysis!, error:"+ e);
			throw e;
		}		

		return "Strategy Analysis executed, list";	
	}		

	
}
