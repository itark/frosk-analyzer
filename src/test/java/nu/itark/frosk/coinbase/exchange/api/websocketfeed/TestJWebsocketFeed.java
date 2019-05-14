package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Channels;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Subscribe;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJWebsocketFeed {

	@Autowired
	WebsocketFeed websocketFeed;
	
	@Autowired
	Observations observations;
	
	@Test
	public void testWebsocketSubscribeOrderReceived() throws InterruptedException {
	
		
        log.info("*** Subscribing ***");
        websocketFeed.subscribeOrderReceived(new Subscribe());		
        
        Thread.sleep(20000);
        
        log.info("*** ready sleeping ***");
		
	}	
	
	
	@Test
	public void testWebsocketSubscribe() throws InterruptedException {
		String[] productIds = new String[]{"BTC-EUR"}; // make this configurable.
	
		
        log.info("*** Subscribing ***");
        websocketFeed.subscribe(new Subscribe(productIds));		
		
        Thread.sleep(30000);
        
        log.info("*** ready sleeping ***");
		
	}
	
		
	@Test
	public void testWebsocketSubscribeonChannel() throws InterruptedException {
		String[] productIds = new String[]{"BTC-EUR"}; // make this configurable.
	
		
		log.info("*** Subscribing ***");


		Channels[] channel = new Channels[1]; 
		channel[0] = new Channels();
		channel[0].setName("full");
		channel[0].setProduct_ids(productIds);

		Subscribe subscribeChannel = new Subscribe(channel);

        websocketFeed.subscribeOnChannel(subscribeChannel);		


        Thread.sleep(20000);
        
        log.info("*** ready sleeping ***");
		
	}



}
