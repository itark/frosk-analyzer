package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

import lombok.Data;

/**
 * Channel
 */
@Data
public class Channels {

    private String name;
    private String[] product_ids;
    
}