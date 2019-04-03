package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Observations {

	List<OrderItem> best50Bids;	
	List<OrderItem> best50Asks;
	
	static int DEFAULT_QUEUE_SIZE = 10;

	Queue<OrderReceived> bestBids = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	
	Queue<OrderReceived> bestAsks = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	
	
	
	@Autowired
	LimitOrderImbalance limitOrderImbalance ;
	
	/**
	 * This one is synchronous.
	 * 
	 * Using fifo with best 50 bids and asks
	 * 
	 * @param orderReceived
	 * @return
	 */
	public Double calculateLimitOrderImbalance() {
		
//		orderReceived.get
		
//   		List<OrderItem> best50Asks = best50.getAsks();   
//   		List<OrderItem> best50Bids = best50.getBids();  		
//		
//		limitOrderImbalance.calculate(midMarket, best50)
		
		//1. Ta fifo och kolla max i den

		//2. Sedan lägg till om högre

		//3. Sedan calcLimitorderImb.
		
		return null;
		
	}
	
	/**
	 * Sync best bids and ask in queues
	 * 
	 * @param orderReceived
	 */
	public void synchronizeBest(OrderReceived orderReceived) {
		log.info("::synchronizeBest {} ::", DEFAULT_QUEUE_SIZE);
		if ("sell".equals(orderReceived.getSide())) {
			 synchronizeBestBids(orderReceived);
		} else if ("buy".equals(orderReceived.getSide())) {
			 synchronizeBestAsks(orderReceived);
		}
	}	
	
	private void synchronizeBestBids(OrderReceived orderReceived) {
//		log.info("::synchronizeBestBids :: {}", bestBids.size());
		if (bestBids.isEmpty()) {
			bestBids.add(orderReceived);
			
			return;
		}
		bestBids.forEach(order -> {
			if ( orderReceived.getPrice().compareTo( order.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestBids.add(orderReceived);
				return;
			} 
		});
	}
	
	private void synchronizeBestAsks(OrderReceived orderReceived) {
//		log.info("::synchronizeBestAsks ::{}", bestAsks.size());
		if (bestAsks.isEmpty()) {
			bestAsks.add(orderReceived);
			return;
		}
		bestAsks.forEach(order -> {
			if ( orderReceived.getPrice().compareTo( order.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestAsks.add(orderReceived);
				return;
			} 
		});		
		
	}	
	
	private Double getMaxPrice(List<OrderItem> best50Asks) {

		OrderItem maxOrderItem = best50Asks
				      .stream()
				      .max(Comparator.comparing(OrderItem::getPrice))
				      .orElseThrow(NoSuchElementException::new);		
		
		return maxOrderItem.getPrice().doubleValue();
		
		
	}
	
	
}
