package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.changedetection.LimitOrderImbalance;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderReceived;
import nu.itark.frosk.crypto.coinbase.MarketDataProxy;

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

	static int DEFAULT_QUEUE_SIZE = 100;
    static String LEVEL_1 = "1";

	Queue<OrderReceived> bestBidsQueue = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	
	Queue<OrderReceived> bestAsksQueue = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	
	
	
	@Autowired
	LimitOrderImbalance limitOrderImbalance ;
	
	
    @Autowired
    MarketDataProxy marketDataProxy;
    


	String productId = null;
	
	BigDecimal midMarketPrice = null;
    
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	public void setMidMarketPrice(BigDecimal midMarketPrice) {
		this.midMarketPrice = midMarketPrice;
	}
	
	/**
	 * This one is synchronous.
	 * 
	 * Using fifo with best 50 bids and asks
	 * 
	 * @param orderReceived
	 * @return
	 */
	public Double calculateLimitOrderImbalance() {
//		log.info("::calculateLimitOrderImbalance  ::");
		Assert.notNull("productId can not be null",productId);
		if (bestBidsQueue.size() < DEFAULT_QUEUE_SIZE && bestAsksQueue.size() < DEFAULT_QUEUE_SIZE) {
			return null;
		}
		
		if (midMarketPrice == null) {
			midMarketPrice = marketDataProxy.getMarketDataTicker(productId).getPrice();
		}
		
		Double loi = limitOrderImbalance.calculate(midMarketPrice, bestBidsQueue, bestAsksQueue);
		
//		log.info("loi {}", loi);
		
		return loi;
		
	}
	
	/**
	 * Sync best bids and ask in queues
	 * 
	 * @param orderReceived
	 */
	public void synchronizeBest(OrderReceived orderReceived) {
//		log.info("::synchronizeBest {} ::", DEFAULT_QUEUE_SIZE);
		if ("sell".equals(orderReceived.getSide())) {
			 synchronizeBestBids(orderReceived);
		} else if ("buy".equals(orderReceived.getSide())) {
			 synchronizeBestAsks(orderReceived);
		}
	}	
	
	private void synchronizeBestBids(OrderReceived orderReceived) {
//		log.info("::synchronizeBestBids :: {}", bestBidsQueue.size());
		if (bestBidsQueue.isEmpty()) {
			bestBidsQueue.add(orderReceived);
			
			return;
		}
		bestBidsQueue.forEach(order -> {
			if ( orderReceived.getPrice().compareTo( order.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestBidsQueue.add(orderReceived);
				return;
			} 
		});
	}
	
	private void synchronizeBestAsks(OrderReceived orderReceived) {
//		log.info("::synchronizeBestAsks ::{}", bestAsksQueue.size());
		if (bestAsksQueue.isEmpty()) {
			bestAsksQueue.add(orderReceived);
			return;
		}
		bestAsksQueue.forEach(order -> {
			if ( orderReceived.getPrice().compareTo( order.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestAsksQueue.add(orderReceived);
				return;
			} 
		});		
		
	}	
	
	private Double getMaxBestAskPrice() {
		OrderReceived order = bestAsksQueue
				.stream().max(Comparator.comparing(OrderReceived::getPrice))
				.orElseThrow(NoSuchElementException::new);

		return order.getPrice().doubleValue();

	}	

	private Double getMaxBestBidPrice() {
		OrderReceived order = bestBidsQueue
				.stream().max(Comparator.comparing(OrderReceived::getPrice))
				.orElseThrow(NoSuchElementException::new);

		return order.getPrice().doubleValue();

	}		
	
	
}
