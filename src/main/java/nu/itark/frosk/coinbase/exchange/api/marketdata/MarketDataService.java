package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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


    public Ticker getMarketDataTicker(String productId) {
        String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/ticker";
        return exchange.get(marketDataEndpoint, new ParameterizedTypeReference<Ticker>(){});
    }

 
    //TODO
    // https://api.gdax.com/products/ETH-EUR/candles?start=2017-07-02T15:25:00.00000Z&end=2017-07-02T16:12:00.00000Z&granularity=3600
    public List<HistoricRate> getMarketDataCandles(String productId, String start, String end, String granularity) {
    	Assert.notNull("start can not be null",start);
    	Assert.notNull("end can not be null",end);
       	Assert.notNull("granularity can not be null",granularity);

    	String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/candles";
        
		marketDataEndpoint += "?start=" + start + "&end=" + end + "&granularity=" + granularity;
       
        return exchange.getAsList(marketDataEndpoint, new ParameterizedTypeReference<HistoricRate[]>(){});
    }
    
}



