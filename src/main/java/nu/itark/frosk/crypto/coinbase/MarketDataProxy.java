package nu.itark.frosk.crypto.coinbase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;
import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketDataService;

@Service
public class MarketDataProxy {

    @Autowired
    MarketDataService marketDataService;
	
    public MarketData getMarketDataOrderBook(String productId, String level) {
        return marketDataService.getMarketDataOrderBook(productId, level);
    }    
    
	public String helloWorld() {
		return "Hello";
	}
	
}
