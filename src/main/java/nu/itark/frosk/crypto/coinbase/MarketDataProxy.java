package nu.itark.frosk.crypto.coinbase;

import java.util.List;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
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
