package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.joda.time.DateTime;

import lombok.Data;

@Data
public class Ticker {

    Long trade_id;
    BigDecimal price;
    BigDecimal size;
    BigDecimal bid;
    BigDecimal ask;
    BigDecimal volume;
	LocalDateTime time;
    
    
 /*   
    {
    	  "trade_id": 4729088,
    	  "price": "333.99",
    	  "size": "0.193",
    	  "bid": "333.98",
    	  "ask": "333.99",
    	  "volume": "5957.11914015",
    	  "time": "2015-11-14T20:46:03.511254Z"
    	}  
 */   
    
    
    
    public Ticker() {}

    public Ticker(Long trade_id, BigDecimal price, BigDecimal size,  BigDecimal bid, BigDecimal ask, LocalDateTime time, BigDecimal volume  ) {
        this.trade_id = trade_id;
        this.price = price;
        this.size = size;
        this.bid = bid;
        this.ask = ask;
        this.volume = volume;
    	this.time = time;
        
    }	
	
	
	
}
