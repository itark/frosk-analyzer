package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

import lombok.Data;

/**
 * Channel
 */
@Data
public class Channel {

    private String name;
    private String[] product_ids = new String[]{"BTC-EUR"};
    
}