package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TestJWebsocketFeed extends BaseIntegrationTest {

	WebsocketFeed websocketFeed;
	ObjectMapper objectMapper = new ObjectMapper();

	@Value("${exchange.api.baseUrl}")
	String baseUrl;
	@Value("${exchange.key}")
	String key;
	@Value("${exchange.passphrase}")
	String passphrase;
	@Value("${exchange.secret}")
	String secret;
	@Value("${gui.enabled}")
	boolean gui;

	@BeforeEach
	void setUp() {
//		websocketFeed = new WebsocketFeed(
//				baseUrl,
//				key,
//				passphrase,
//				gui,
//				new Signature(secret),
//				objectMapper);
	}

	@Test
	public void testWebsocketSubscribeonChannel() throws InterruptedException {
/*
		String[] productIds = new String[]{"BTC-EUR"}; // make this configurable.
		Subscribe subscribeChannel = new Subscribe(productIds);
		websocketFeed.subscribe(subscribeChannel);
		Thread.sleep(1000000);
*/
	}

}
