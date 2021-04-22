package nu.itark.frosk.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.logging.Logger;

@Controller
public class GreetingController {

    Logger logger = Logger.getLogger(GreetingController.class.getName());

    @MessageMapping("/hello")
    @SendTo("/topic/price")
    public String greeting(String message) throws Exception {


        logger.info("helllo log..");


        return "Hello World!";
    }



}
