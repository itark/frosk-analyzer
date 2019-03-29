package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.changedetection.LimitOrderImbalance;
import nu.itark.frosk.coinbase.exchange.api.marketdata.OrderItem;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderReceived;

/**
 * This class observes websocketfeed and do calculations.
 * 
 * 
 * @author fredrikmoller
 *
 */
@Service
public class Observations {

	List<OrderItem> best50Bids;	
	List<OrderItem> best50Asks;


	Queue<Integer> fifo = new CircularFifoQueue<Integer>(2);	
	
	
	
	@Autowired
	LimitOrderImbalance limitOrderImbalance ;
	
	
	public Double calculateLimitOrderImbalance(OrderReceived orderReceived) {
		
//		orderReceived.get
		
//   		List<OrderItem> best50Asks = best50.getAsks();   
//   		List<OrderItem> best50Bids = best50.getBids();  		
//		
//		limitOrderImbalance.calculate(midMarket, best50)
		
		
		
		return null;
		
	}
	
	
	private Double getMaxPrice(List<OrderItem> best50Asks) {

		OrderItem minOrderItem = best50Asks
				      .stream()
				      .max(Comparator.comparing(OrderItem::getPrice))
				      .orElseThrow(NoSuchElementException::new);		
		
		return minOrderItem.getPrice().doubleValue();
		
		
	}
	
	
}
