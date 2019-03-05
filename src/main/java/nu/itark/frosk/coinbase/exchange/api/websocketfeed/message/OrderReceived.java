package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Data;

/**
 * 
 * {
 *   "type": "received",
 *   "time": "2014-11-07T08:19:27.028459Z",
 *   "product_id": "BTC-USD",
 *   "sequence": 10,
 *   "order_id": "d50ec984-77a8-460a-b958-66f114b0de9b",
 *   "size": "1.34",
 *   "price": "502.1",
 *   "side": "buy",
 *   "order_type": "limit"
*}
 * 
 * 
 * 
 * @author fredrikmoller
 *
 */

@Data
public class OrderReceived {

    String type;
    String time;
    String product_id;
    Long sequence;
    String order_id;
    BigDecimal size;
    BigDecimal price;
    String side;
    String order_type;
    
    String client_oid;
	
	
    /**
	* Market orders (indicated by the order_type field) may have an optional funds field which indicates how much quote currency will be used to buy or sell. 
	* For example, a funds field of 100.00 for the BTC-USD product would indicate a purchase of up to 100.00 USD worth of bitcoin.
     */
	public enum OrderTypeEnum {
		LIMIT("limit"),
		MARKET("market");

		private String value;

		OrderTypeEnum(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}
	
	@Override
	public String toString(){
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
    
    
}
