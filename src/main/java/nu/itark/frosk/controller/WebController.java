package nu.itark.frosk.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
	
	/**
	* @Example  http://localhost:8080/frosk-analyzer-0.0.1/initDatabase
	 */
	@RequestMapping(value="initDatabase", method={RequestMethod.GET, RequestMethod.POST})
	@ResponseBody
	public String initDatabase(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WebController.class.getName());
		logger.log(Level.INFO, "initDatabases");
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
		dataManager.insertSecurityPricesIntoDatabase(Database.YAHOO, true);
		
		return "Security prices inserted";	
	}	
	
	
}
