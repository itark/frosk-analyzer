package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderReceived;

@SpringBootTest
@Slf4j
public class TestJObservations {

	@Autowired
	Observations  observations;
	
	
	@Test
	final public void testQueue() {

		OrderBookMessage orderReceived = new OrderBookMessage();
		orderReceived.setSize(new BigDecimal(10));
		orderReceived.setPrice(new BigDecimal(3567));
		
		List<Double> lois = new ArrayList<Double>();
		
		for (int i = 0; i < 5; i++) {
			orderReceived.setSize(new BigDecimal(10).add(new BigDecimal(i)));
			orderReceived.setPrice(new BigDecimal(3567).add(new BigDecimal(i)));
			
			observations.synchronizeBest(orderReceived);
			
		}
		
		lois.add(observations.calculateLimitOrderImbalance("BTC-EUR"));
		
		lois.forEach(loi -> {
			
			log.info("loi {}", loi);			
			
		});
		

	}


	@Test
	public void testBuy() {
		BigDecimal price = new BigDecimal(9342.34);
		observations.buy(BigDecimal.ONE);

		System.out.println(observations.profitAndLoss);

	}

	@Test
	public void testBuy2() {
		BigDecimal profitAndLoss = BigDecimal.ZERO;
		BigDecimal amount = BigDecimal.ONE;
		BigDecimal price = new BigDecimal(9342.34);
		BigDecimal value = amount.multiply(price);
		profitAndLoss = profitAndLoss.add(value);

		System.out.println("PaL:"+profitAndLoss);

	}

}
