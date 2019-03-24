package nu.itark.frosk.changedetection;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;
import nu.itark.frosk.coinbase.exchange.api.marketdata.OrderItem;

@Service
@Slf4j
public class LimitOrderImbalance {

	
	/**
	 * Doing the page 16 stuff
	 * 
	 * @param midMarket
	 * @param best50
	 * @return
	 */
	public Double calculate(MarketData midMarket, MarketData best50) {
		
	
   		List<OrderItem> midMarketAsks = midMarket.getAsks();   
   		List<OrderItem> midMarketBids = midMarket.getBids();   	
 		BigDecimal midMarketPrice = ( midMarketBids.get(0).getPrice().add(midMarketAsks.get(0).getPrice()) ).divide(BigDecimal.valueOf(2));
 		  
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
	
}
