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
}
