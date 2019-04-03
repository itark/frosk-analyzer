package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderReceived;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJObservations {

	@Autowired
	Observations  observations;
	
	
	@Test
	final public void testQueue() {
		
		OrderReceived orderReceived = new OrderReceived();
		orderReceived.setSize(new BigDecimal(10));
		orderReceived.setPrice(new BigDecimal(3567));
		
		List<Double> lois = new ArrayList<Double>();
		
		for (int i = 0; i < 5; i++) {
			orderReceived.setSize(new BigDecimal(10).add(new BigDecimal(i)));
			orderReceived.setPrice(new BigDecimal(3567).add(new BigDecimal(i)));
			
			observations.synchronizeBest(orderReceived);
			
		}
		
		
		lois.add(observations.calculateLimitOrderImbalance());
		
		lois.forEach(loi -> {
			
			log.info("loi {}", loi);			
			
		});
		

		
		
	}
}
