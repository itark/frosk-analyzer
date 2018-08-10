package nu.itark.frosk.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import nu.itark.frosk.dataset.BITFINEXDataManager;
import nu.itark.frosk.dataset.DataManager;
import nu.itark.frosk.dataset.Database;

@Controller
public class WebController {
	Logger logger = Logger.getLogger(WebController.class.getName());

	@Autowired
	BITFINEXDataManager bitfinexManager;	
	
	@Autowired
	DataManager dataManager;		
	
	@Value("${welcome.message:test}")
	private String message;

	@RequestMapping("/")
	public String welcome2(Map<String, Object> model) {
		logger.log(Level.INFO, "index.jsp!, message="+this.message);
		model.put("message", this.message);
		return "index";	
	
	}	
	
	@RequestMapping("/strategies")
	public String strategies(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "/strategies");
		model.put("message", this.message);
		return "strategies";	
	}		
	
	@RequestMapping("/rnn")
	public String rnn(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "/rnn");
		model.put("message", this.message);
		return "rnn";	
	}	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer-0.0.1/fill?database=BITFINEX
	 */
	@RequestMapping(value="fill", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String fill(@RequestParam("database") String database, Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "/fill?database="+database);
		model.put("message", this.message);
		long count = 0;
		if (database.equals(Database.BITFINEX.toString())) {
			logger.log(Level.INFO, "Syncronizing BITFINEX...");
			count = bitfinexManager.syncronize();
			
		}
		
		return "Updated: "+count+" rows";	
	}	
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer-0.0.1/initDatabase
	 */
	@RequestMapping(value="initDatabase", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String initDatabase(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "initDatabases");
		model.put("message", this.message);
		dataManager.insertSecuritiesIntoDatabase();
			
		
		return "Securities inserted";	
	}
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer-0.0.1/getPrice
	 */
	@RequestMapping(value="getPrices", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String getPrices(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "getPrices");
		model.put("message", this.message);
		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO, true);
			
		
		return "Security prices inserted";	
	}	
	
	
}
