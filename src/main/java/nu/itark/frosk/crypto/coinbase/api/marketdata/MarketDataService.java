package nu.itark.frosk.crypto.coinbase.api.marketdata;

import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * Created by robevansuk on 07/02/2017.
 */
public class MarketDataService {

    final Coinbase exchange;

    public MarketDataService(final Coinbase exchange) {
        this.exchange = exchange;
    }

    public static final String PRODUCT_ENDPOINT = "/products";

    public static final String ORDER_ENDPOINT = "/order";

    public MarketData getMarketDataOrderBook(String productId, int level) {
        String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/book";
        if(level != 1)
            marketDataEndpoint += "?level=" + level;
       return exchange.get(marketDataEndpoint, new ParameterizedTypeReference<MarketData>(){});
    }

    public List<Trade> getTrades(String productId) {
        String tradesEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/trades";
        return exchange.getAsList(tradesEndpoint, new ParameterizedTypeReference<Trade[]>(){});
    }


/*
    */
/**
     * https://docs.cloud.coinbase.com/advanced-trade-api/reference/retailbrokerageapi_postorder
     * @param productId
     * @return
     *
     * https://api.coinbase.com/api/v3/brokerage/orders
     *
     *
     *//*

    public Order createOrder(String productId) {
        //TODO fix dummy impl.
        String tradesEndpoint = ORDER_ENDPOINT + "/" + productId + "/trades";
        Order order = new Order();
        order.setOrderId("666");
        return order;
   }
*/

}
