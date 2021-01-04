package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
	BigDecimal profitAndLoss = BigDecimal.ZERO;
	BigDecimal amount = BigDecimal.ONE;

	
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

		boolean change_high = changeDetector.isChangeHigh();
		boolean change_low = changeDetector.isChangeLow();
//		boolean change = changeDetector.isChange();

		//sendCusumHighMessage(changeDetector.cusumHigh().toString(),LocalDateTime.now().toString());
		//		sendLoiMessage(loi.toString(),LocalDateTime.now().toString());

		if(change_high) {
			log.info("POSITIVE CHANGE DETECTED! price:{}, side:{} , mid market price:{} ", orderReceived.getPrice().toString(),orderReceived.getSide(), midMarketPrice);
//			log.info("isUp {} ", isUp(orderReceived.getPrice()));

//			calculateProfitandLoss(orderReceived.getPrice(), true);
			changeDetector.reset();

		}

		if(change_low) {
			log.info("NEGATIVE CHANGE DETECTED! price:{}, side:{} , mid market price:{} ", orderReceived.getPrice().toString(),orderReceived.getSide(), midMarketPrice);
//			log.info("isUp {} ", isUp(orderReceived.getPrice()));

//			calculateProfitandLoss(orderReceived.getPrice(), false);
			changeDetector.reset();
		}



//		sendCusumHighMessage(changeDetector.cusumHigh().toString(),LocalDateTime.now().toString());
//		sendCusumLowMessage(changeDetector.cusumLow().toString(),LocalDateTime.now().toString());



	}

	private void calculateProfitandLoss(BigDecimal orderReceivedPrice, boolean buy) {
		if (buy) {
			buy(orderReceivedPrice);
			//buy
		} else {
			//sell
			sell(orderReceivedPrice);
		}

//		profitAndLoss

	log.info("profitAndLoss="+profitAndLoss);

	}


	void buy(BigDecimal orderReceivedPrice ) {
		profitAndLoss = profitAndLoss.add(amount.multiply(midMarketPrice));
	}

	void sell(BigDecimal orderReceivedPrice) {
		if (!profitAndLoss.equals(BigDecimal.ZERO) ) {
			profitAndLoss = profitAndLoss.subtract(amount.multiply(midMarketPrice));
		}
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

	protected void sendCusumHighMessage(String cusum_high, String time)  {
		log.info("::sendCusumMessage::cusum_high {} ",cusum_high);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"cusum_high\",\"value\":\"%s\",\"time\":\"%s\"}", cusum_high, time)));

			}

		} catch (IOException e) {
			log.error("Could not send cusum message", e);
		}
	}

	protected void sendCusumLowMessage(String cusum, String time)  {
		//log.info("::sendCusumMessage::cusum {} ",cusum);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"cusum_low\",\"value\":\"%s\",\"time\":\"%s\"}", cusum, time)));

			}

		} catch (IOException e) {
			log.error("Could not send cusum message", e);
		}
	}


	protected void sendLoiMessage(String loi, String time)  {
		//log.info("::sendCusumMessage::cusum {} ",cusum);
		try {
			if (webSocketsessionPrice != null) {
				webSocketsessionPrice.sendMessage(new TextMessage(String.format("{\"type\":\"loi\",\"value\":\"%s\",\"time\":\"%s\"}", loi, time)));

			}

		} catch (IOException e) {
			log.error("Could not send loi message", e);
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
