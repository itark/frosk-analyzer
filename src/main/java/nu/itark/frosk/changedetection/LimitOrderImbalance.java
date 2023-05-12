package nu.itark.frosk.changedetection;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import nu.itark.frosk.crypto.coinbase.api.marketdata.MarketData;
import nu.itark.frosk.crypto.coinbase.api.marketdata.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

@Service
@Slf4j
public class LimitOrderImbalance {

	
	/**
	 * Doing the page 16 stuff.
	 * 
	 * Relates to 1 observation but top 50 instead of 20 top
	 * 
	 * @param midMarket
	 * @param best50
	 * @return limit order imbalance at this observation
	 * @deprecated
	 */
	public Double calculate(MarketData midMarket, MarketData best50) {
		
	
   		List<OrderItem> midMarketAsks = midMarket.getAsks();
   		List<OrderItem> midMarketBids = midMarket.getBids();   	
 		BigDecimal midMarketPrice = ( midMarketBids.get(0).getPrice().add(midMarketAsks.get(0).getPrice()) ).divide(BigDecimal.valueOf(2));

		System.out.println("midMarketPrice:"+midMarketPrice);

 		  
   		List<OrderItem> best50Asks = best50.getAsks();   
   		List<OrderItem> best50Bids = best50.getBids();   	

   		Double limitOrderImbalanceMeasurement = 0.0;
   		Double leftRoof = 0.0;
   		Double leftFloor = 0.0;
   		Double rigthRoof = 0.0;
   		Double rightFloor  = 0.0;
   		
   		
   		for (OrderItem orderItemBid : best50Bids) {
   			leftRoof += (orderItemBid.getPrice().doubleValue() * orderItemBid.getSize().doubleValue());
   			leftFloor += orderItemBid.getSize().doubleValue();
		}
   		
   		for (OrderItem orderItemAsk : best50Asks) {
   			rigthRoof += (orderItemAsk.getPrice().doubleValue() * orderItemAsk.getSize().doubleValue());
   			rightFloor += orderItemAsk.getSize().doubleValue();
		}

//   		log.info("leftRoof {} ", leftRoof);
//  		log.info("leftFloor {} ", leftFloor); 		
//   		log.info("rigthRoof {} ", rigthRoof);
//  		log.info("rightFloor {} ", rightFloor); 	  		
//  		log.info("midMarketPrice {} ", midMarketPrice.doubleValue()); 	   		
   		
   		limitOrderImbalanceMeasurement =   ( (leftRoof + rigthRoof) / (leftFloor + rightFloor) ) - midMarketPrice.doubleValue();
   		
		return limitOrderImbalanceMeasurement;
		
	}
	
	
	/**
	 * Doing the page 16 stuff.
	 * 
	 * Relates to 1 observation but top 50 instead of 20 top
	 * 
	 * @param midMarketPrice
	 * @param bestBids
	 * @param bestAsks
	 * @return limit order imbalance at this observation
	 */
	public Double calculate(BigDecimal midMarketPrice, Queue<OrderBookMessage> bestBids, Queue<OrderBookMessage> bestAsks) {

   		Double limitOrderImbalanceMeasurement = 0.0;
   		Double leftRoof = 0.0;
   		Double leftFloor = 0.0;
   		Double rigthRoof = 0.0;
   		Double rightFloor  = 0.0;
   		
   		
   		for (OrderBookMessage orderReceivedBid : bestBids) {
   			leftRoof += (orderReceivedBid.getPrice().doubleValue() * orderReceivedBid.getRemaining_size().doubleValue());
   			leftFloor += orderReceivedBid.getRemaining_size().doubleValue();
		}
   		
   		for (OrderBookMessage orderReceivedAsk : bestAsks) {
   			rigthRoof += (orderReceivedAsk.getPrice().doubleValue() * orderReceivedAsk.getRemaining_size().doubleValue());
   			rightFloor += orderReceivedAsk.getRemaining_size().doubleValue();
		}

//   		log.info("leftRoof {} ", leftRoof);
//  		log.info("leftFloor {} ", leftFloor); 		
//   		log.info("rigthRoof {} ", rigthRoof);
//  		log.info("rightFloor {} ", rightFloor); 	  		
//  		log.info("midMarketPrice {} ", midMarketPrice.doubleValue()); 	   		
   		
   		limitOrderImbalanceMeasurement =   ( (leftRoof + rigthRoof) / (leftFloor + rightFloor) ) - midMarketPrice.doubleValue();
 
 		//log.info("loi {} ", limitOrderImbalanceMeasurement);
 		    		
   		
		return limitOrderImbalanceMeasurement;
		
	}
	
	
}
