package nu.itark.frosk.crypto.coinbase;

import nu.itark.frosk.crypto.coinbase.api.marketdata.MarketData;
import nu.itark.frosk.crypto.coinbase.api.marketdata.MarketDataService;
import nu.itark.frosk.crypto.coinbase.api.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class MarketDataProxy {

	@Autowired
    MarketDataService marketDataService;

    public MarketData getMarketDataOrderBook(String productId, int level) {
        return marketDataService.getMarketDataOrderBook(productId, level);
    }    
  
    public List<Trade> getTrades(String productId) {
        return marketDataService.getTrades(productId);
    }

}
