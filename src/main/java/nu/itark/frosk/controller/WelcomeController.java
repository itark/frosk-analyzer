package nu.itark.frosk.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WelcomeController {
	Logger logger = Logger.getLogger(WelcomeController.class.getName());

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
		Logger logger = Logger.getLogger(WelcomeController.class.getName());
		logger.log(Level.INFO, "/strategies");
		model.put("message", this.message);
		return "strategies";	
	}		
	
	@RequestMapping("/rnn")
	public String rnn(Map<String, Object> model) {
		Logger logger = Logger.getLogger(WelcomeController.class.getName());
		logger.log(Level.INFO, "/rnn");
		model.put("message", this.message);
		return "rnn";	
	}		
	
}
