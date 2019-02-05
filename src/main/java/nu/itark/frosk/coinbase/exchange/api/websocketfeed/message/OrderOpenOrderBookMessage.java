package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * {
     "type": "open",
     "time": "2014-11-07T08:19:27.028459Z",
     "product_id": "BTC-USD",
     "sequence": 10,
     "order_id": "d50ec984-77a8-460a-b958-66f114b0de9b",
     "price": "200.2",
     "remaining_size": "1.00",
     "side": "sell"
     }
 * Created by robevansuk on 15/03/2017.
 */
public class OrderOpenOrderBookMessage extends OrderBookMessage {

	
    @Override
    public String toString() {
    	return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    	
    }

	
}
