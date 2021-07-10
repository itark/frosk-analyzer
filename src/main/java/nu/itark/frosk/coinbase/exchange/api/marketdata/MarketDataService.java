package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import nu.itark.frosk.coinbase.exchange.api.exchange.GdaxExchange;

//@Service
public class MarketDataService {

    @Autowired
    GdaxExchange exchange;

    public static final String PRODUCT_ENDPOINT = "/products";


    /**
     * https://docs.pro.coinbase.com/#get-products
     * 
     * 
     * Get a list of open orders for a product. The amount of detail shown can be customized with the level parameter.
     * 
     * GET /products/<product-id>/book
     * 
     * By default, only the inside (i.e. best) bid and ask are returned. 
     * This is equivalent to a book depth of 1 level. If you would like to see a larger order book, specify the level query parameter.
     * 
     * If a level is not aggregated, then all of the orders at each price will be returned. 
     * Aggregated levels return only one size for each active price (as if there was only a single order for that size at the level).
     * 
     * 	{
     *	    "sequence": "3",
     *	    "bids": [
     *	        [ price, size, num-orders ],
     *	        [ "295.96", "4.39088265", 2 ],
     *	        ...
     *	    ],
     *	    "asks": [
     *	        [ price, size, num-orders ],
     *	        [ "295.97", "25.23542881", 12 ],
     *	        ...
     *	    ]
     *	}
     * 
     * @param productId
     * @param level
     * 	1	Only the best bid and ask
     *	2	Top 50 bids and asks (aggregated)
     *	3	Full order book (non aggregated)
     * @return
     */
    public MarketData getMarketDataOrderBook(String productId, String level) {
        String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/book";
        if(level != null && !level.equals("") && !level.equals("1"))
            marketDataEndpoint += "?level=" + level;
       return exchange.get(marketDataEndpoint, new ParameterizedTypeReference<MarketData>(){});
    }


	/**
	* 	https://docs.pro.coinbase.com/#get-product-ticker	
	* 
	*    Get Product Ticker
	*    {
	*      "trade_id": 4729088,
	*      "price": "333.99",
	*      "size": "0.193",
	*      "bid": "333.98",
	*      "ask": "333.99",
	*      "volume": "5957.11914015",
	*      "time": "2015-11-14T20:46:03.511254Z"
	*    }
	*    Snapshot information about the last trade (tick), best bid/ask and 24h volume.
	*
	*    HTTP REQUEST
	*    GET /products/<product-id>/ticker
	*
	*    REAL-TIME UPDATES
	*    Polling is discouraged in favor of connecting via the websocket stream and listening for match messages.
	*    
	*/    
    public Ticker getMarketDataTicker(String productId) {
        String marketDataEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/ticker";
        return exchange.get(marketDataEndpoint, new ParameterizedTypeReference<Ticker>(){});
    }

 
    
    /**
     * 
     * https://docs.pro.coinbase.com/#get-trades
     * 
     * Get Trades
     * 
     * List the latest trades for a product.
	 *
     * HTTP REQUEST
     * GET /products/<product-id>/trades
     * 
     * 
     * SIDE
     * The trade side indicates the maker order side. The maker order is the order that was open on the order book. buy side indicates a down-tick 
     * because the maker was a buy order and their order was removed. Conversely, sell side indicates an up-tick.
     * 
     * [{
	 *	    "time": "2014-11-07T22:19:28.578544Z",
	 *	    "trade_id": 74,
	 *	    "price": "10.00000000",
	 *	    "size": "0.01000000",
	 *	    "side": "buy"
	 *	}, {
	 *	    "time": "2014-11-07T01:08:43.642366Z",
	 *	    "trade_id": 73,
	 *	    "price": "100.00000000",
	 *	    "size": "0.01000000",
	 *	    "side": "sell"
	 *	}]
     * 
     * 
     * @param productId
     * @return
     */
    public List<Trade> getTrades(String productId) {
        String tradesEndpoint = PRODUCT_ENDPOINT + "/" + productId + "/trades";
        return exchange.getAsList(tradesEndpoint, new ParameterizedTypeReference<Trade[]>(){});
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



