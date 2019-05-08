package nu.itark.frosk.coinbase.exchange.api.websocketfeed.message;

import java.util.List;

import lombok.Data;

/**
 * @author Fredrik Möller
 */
@Data
public class Channels extends OrderBookMessage {

    Channel[] channels;

}
