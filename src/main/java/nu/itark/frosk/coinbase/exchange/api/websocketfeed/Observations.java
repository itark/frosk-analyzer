package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.changedetection.ChangeDetector;
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

	// Queue<OrderOpenOrderBookMessage> bestBidsQueue = new CircularFifoQueue<OrderOpenOrderBookMessage>(DEFAULT_QUEUE_SIZE);	
	// Queue<OrderOpenOrderBookMessage> bestAsksQueue = new CircularFifoQueue<OrderOpenOrderBookMessage>(DEFAULT_QUEUE_SIZE);	

	 Queue<OrderReceived> bestBidsQueue = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	
	Queue<OrderReceived> bestAsksQueue = new CircularFifoQueue<OrderReceived>(DEFAULT_QUEUE_SIZE);	


	// Queue<T> bestBidsQueue = new CircularFifoQueue<T>(DEFAULT_QUEUE_SIZE);	
	// Queue<T> bestAsksQueue = new CircularFifoQueue<T>(DEFAULT_QUEUE_SIZE);	
	

	
	@Autowired
	LimitOrderImbalance limitOrderImbalance ;
	
	
    @Autowired
    MarketDataProxy marketDataProxy;
    

	WebSocketSession webSocketsessionPrice;


    @Autowired
    ChangeDetector<Double> changeDetector;


	String productId = null;
	
	BigDecimal midMarketPrice = null;
    
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	public void setMidMarketPrice(BigDecimal midMarketPrice) {
		this.midMarketPrice = midMarketPrice;
	}
	
	/**
	 * Calculating LOI and use ChangeDetector on it.
	 * 
	 * If change send Cusum
	 * @param orderReceived
	 */
	public void process(OrderReceived orderReceived) {
		Double loi = calculateLimitOrderImbalance();

		if (loi != null) {
			changeDetector.update(loi);
		}

		if(!changeDetector.isReady()) {
			return;
		}

		boolean change = changeDetector.isChange();

		if(change) {
			// log.info("CHANGE DETECTED! Anomalous value: {}, mid market price {} ", orderReceived.getPrice().doubleValue(), midMarketPrice);
			log.info("CHANGE DETECTED! Anomalous value: {}, mid market price {} ", ReflectionToStringBuilder.toString(orderReceived,ToStringStyle.MULTI_LINE_STYLE), midMarketPrice);

			log.info("isUp {} ", isUp(orderReceived.getPrice()));
			// One alarm is enough. This is the new data source now.
			// If it changes again, we want to know.
			
			////sendPriceMessage(orderReceived.getPrice().toPlainString(),orderReceived.getTime());

			changeDetector.reset();
		}

		sendCusumMessage(changeDetector.cusum().toString(),LocalDateTime.now().toString());

		// sendCusumMessage(changeDetector.cusum().toString(),orderReceived.getTime());

	}

	private boolean isUp(BigDecimal price) {
		int compare = price.compareTo(midMarketPrice);
		return (compare == 1) ? true : false;
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
		Assert.notNull("productId can not be null",productId);
		if (bestBidsQueue.size() < DEFAULT_QUEUE_SIZE && bestAsksQueue.size() < DEFAULT_QUEUE_SIZE) {
			return null;
		}
		
		if (midMarketPrice == null) {
			midMarketPrice = marketDataProxy.getMarketDataTicker(productId).getPrice();
		}
		
		Double loi = limitOrderImbalance.calculate(midMarketPrice, bestBidsQueue, bestAsksQueue);
		
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
	

	private void synchronizeBestBids(OrderReceived order) {
//		log.info("::synchronizeBestBids :: {}", bestBidsQueue.size());
		if (bestBidsQueue.isEmpty()) {
			bestBidsQueue.add(order);
			
			return;
		}
		bestBidsQueue.forEach(orderOpen -> {
			if ( order.getPrice().compareTo( orderOpen.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestBidsQueue.add(order);
				return;
			} 
		});
	}
	
	private void synchronizeBestAsks(OrderReceived order) {
//		log.info("::synchronizeBestAsks ::{}", bestAsksQueue.size());
		if (bestAsksQueue.isEmpty()) {
			bestAsksQueue.add(order);
			return;
		}
		bestAsksQueue.forEach(orderOpen -> {
			if ( order.getPrice().compareTo( orderOpen.getPrice() ) == 1) {
//				log.info("orderReceived.getPrice(): {} greater then {}",orderReceived.getPrice(), order.getPrice() );
				bestAsksQueue.add(order);
				return;
			} 
		});		
		
	}	
	
	protected void sendPriceMessage(String price, String time)  {
		//log.info("price {}, time {}",price, time);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"price\",\"price\":\"%s\",\"time\":\"%s\"}", price, time)));
			}

		} catch (IOException e) {
			log.error("Could not send price message", e);
		}
	}

	protected void sendCusumMessage(String cusum, String time)  {
		//log.info("::sendCusumMessage::cusum {} ",cusum);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"cusum\",\"value\":\"%s\",\"time\":\"%s\"}", cusum, time)));

			}

		} catch (IOException e) {
			log.error("Could not send  message", e);
		}
	}

	protected void sendLoiMessage(String loi, String time)  {
		//log.info("::sendCusumMessage::cusum {} ",cusum);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"loi\",\"value\":\"%s\",\"time\":\"%s\"}", loi, time)));

			}

		} catch (IOException e) {
			log.error("Could not send price message", e);
		}
	}



	// private Double getMaxBestAskPrice() {
	// 	OrderReceived order = bestAsksQueue
	// 			.stream().max(Comparator.comparing(OrderReceived::getPrice))
	// 			.orElseThrow(NoSuchElementException::new);

	// 	return order.getPrice().doubleValue();

	// }	

	// private Double getMaxBestBidPrice() {
	// 	OrderReceived order = bestBidsQueue
	// 			.stream().max(Comparator.comparing(OrderReceived::getPrice))
	// 			.orElseThrow(NoSuchElementException::new);

	// 	return order.getPrice().doubleValue();

	// }

	/**
	 * @return the bestBidsQueue
	 */
	public Queue<OrderReceived> getBestBidsQueue() {
		return bestBidsQueue;
	}

	/**
	 * @param bestBidsQueue the bestBidsQueue to set
	 */
	public void setBestBidsQueue(Queue<OrderReceived> bestBidsQueue) {
		this.bestBidsQueue = bestBidsQueue;
	}

	/**
	 * @return the bestAsksQueue
	 */
	public Queue<OrderReceived> getBestAsksQueue() {
		return bestAsksQueue;
	}

	/**
	 * @param bestAsksQueue the bestAsksQueue to set
	 */
	public void setBestAsksQueue(Queue<OrderReceived> bestAsksQueue) {
		this.bestAsksQueue = bestAsksQueue;
	}

	/**
	 * @return the limitOrderImbalance
	 */
	public LimitOrderImbalance getLimitOrderImbalance() {
		return limitOrderImbalance;
	}

	/**
	 * @param limitOrderImbalance the limitOrderImbalance to set
	 */
	public void setLimitOrderImbalance(LimitOrderImbalance limitOrderImbalance) {
		this.limitOrderImbalance = limitOrderImbalance;
	}

	/**
	 * @return the marketDataProxy
	 */
	public MarketDataProxy getMarketDataProxy() {
		return marketDataProxy;
	}

	/**
	 * @param marketDataProxy the marketDataProxy to set
	 */
	public void setMarketDataProxy(MarketDataProxy marketDataProxy) {
		this.marketDataProxy = marketDataProxy;
	}

	/**
	 * @param webSocketsession the webSocketsessionPrice to set
	 */
	public void setWebSocketsession(WebSocketSession webSocketsession) {
		this.webSocketsessionPrice = webSocketsession;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the midMarketPrice
	 */
	public BigDecimal getMidMarketPrice() {
		return midMarketPrice;
	}

	/**
	 * @return the webSocketsessionPrice
	 */
	public WebSocketSession getWebSocketsessionPrice() {
		return webSocketsessionPrice;
	}
	
	
}
