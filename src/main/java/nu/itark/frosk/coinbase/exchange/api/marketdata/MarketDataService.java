package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import nu.itark.frosk.coinbase.exchange.api.exchange.GdaxExchange;

/**
 * Created by robevansuk on 07/02/2017.
 */
@Service
public class MarketDataService {

    @Autowired
    GdaxExchange exchange;

    public static final String PRODUCT_ENDPOINT = "/products";

    public MarketData getMarketDataOrderBook(String productId, String level) {
        String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/book";
        if(level != null && !level.equals("") && !level.equals("1"))
            marketDataEndpoint += "?level=" + level;
       return exchange.get(marketDataEndpoint, new ParameterizedTypeReference<MarketData>(){});
    }

    public List<Trade> getTrades(String productId) {
        String tradesEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/trades";
        return exchange.getAsList(tradesEndpoint, new ParameterizedTypeReference<Trade[]>(){});
    }
}
