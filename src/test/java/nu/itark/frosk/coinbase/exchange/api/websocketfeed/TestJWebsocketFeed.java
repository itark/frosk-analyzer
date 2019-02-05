package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Subscribe;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJWebsocketFeed {

	@Autowired
	WebsocketFeed websocketFeed;
	
	
	@Test
	public void testWebsocket() throws InterruptedException {
		String[] productIds = new String[]{"BTC-GBP", "ETH-BTC"}; // make this configurable.
	
		
        log.info("*** Subscribing ***");
        websocketFeed.subscribe(new Subscribe(productIds));		
		
        Thread.sleep(20000);
        
        log.info("*** ready sleeping ***");
		
	}
	
	
}