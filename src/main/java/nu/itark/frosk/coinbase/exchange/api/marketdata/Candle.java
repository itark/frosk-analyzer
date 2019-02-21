package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Candle {

	//TODO fel form, se curl i noter
	
	BigDecimal time;	
    BigDecimal low;
    BigDecimal high;
    BigDecimal open;
    BigDecimal close;
    BigDecimal volume;
	
    public Candle() {}

//    public Candle(LocalDateTime time,  BigDecimal low,  BigDecimal high,  BigDecimal open,  BigDecimal close,  BigDecimal volume) {
//    	this.time = time;	
//        this.low = low;
//        this.high = high;
//        this.open = open;
//        this.close = close;
//        this.volume = volume;
//    }  
    
    
	
	
}
