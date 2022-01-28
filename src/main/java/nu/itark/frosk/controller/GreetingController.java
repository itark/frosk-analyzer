package nu.itark.frosk.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.logging.Logger;

@Controller
public class GreetingController {

    Logger logger = Logger.getLogger(GreetingController.class.getName());


    /**
     *  curl -XGET http://localhost:8080/hello
     *
     * @param message
     * @return
     * @throws Exception
     */

    @MessageMapping("/hello")
    @SendTo("/topic/price")
    public String greeting(String message) throws Exception {


        logger.info("helllo log..");


        return "Hello World!";
    }



}
