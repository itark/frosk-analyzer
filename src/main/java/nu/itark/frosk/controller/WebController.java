package nu.itark.frosk.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

	@Autowired
	DataManager dataManager;		

	@Autowired
	StrategyAnalysis strategyAnalysis;		
	
	@Autowired
	HighLander highLander;

	@GetMapping({"/"})
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
	
	@GetMapping({"/ma"})
	public String ma(Map<String, Object> model) {
		return "ma";	
	}

	@GetMapping({"/sma"})
	public String sma(Map<String, Object> model) {
		return "sma";
	}

	@GetMapping({"/engulfing"})
	public String engulfing(Map<String, Object> model) {
		return "engulfing";
	}

	@RequestMapping("/all")
	public String all(Map<String, Object> model) {
		return "all";	
	}		

	@RequestMapping("/echo")
	public String echo(Map<String, Object> model) {
		return "echo";	
	}		

	@RequestMapping("/echo2")
	public String echo2(Map<String, Object> model) {
		return "echo2";	
	}	

	@RequestMapping("/loi")
	public String loi(Map<String, Object> model) {
		return "loi";	
	}	


	@RequestMapping("/snake")
	public String rrswebsocket(Map<String, Object> model) {
		return "snake";	
	}		
	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/highlander
	 */
	@RequestMapping("/highlander")
	@ResponseBody
	public String runHighlander(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "highlander - COINBASE");
		
		highLander.runCleanInstall(Database.COINBASE);
		
		return "Highlander - COINBASE executed";
	}
	
	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer/initDatabase
	 */
	@RequestMapping("/initDatabase")
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
		logger.log(Level.INFO, "fill , COINBASE");

		dataManager.addSecurityPricesIntoDatabase(Database.COINBASE);
		//dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);

		return "Security prices inserted from :"+ Database.COINBASE + " and "+Database.YAHOO ;
	}	

	/**
	* @Example  http://localhost:8080/frosk-analyzer/run
	 */
	@RequestMapping(value="run", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String run(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "run , YAHOO and COINBASE");
	
		try {
			strategyAnalysis.run(null, null);
		} catch (DataIntegrityViolationException e) {
			logger.severe("Error running StrategyAnalysis!, error:"+ e);
			throw e;
		}		

		return "Strategy Analysis executed";
	}		

	
}
