package nu.itark.frosk.crypto.coinbase;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.itark.frosk.coinbase.exchange.api.marketdata.HistoricRate;
import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;
import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketDataService;
import nu.itark.frosk.coinbase.exchange.api.marketdata.Ticker;
import nu.itark.frosk.coinbase.exchange.api.marketdata.Trade;

@Service
public class MarketDataProxy {

    @Autowired
    MarketDataService marketDataService;
	
    public MarketData getMarketDataOrderBook(String productId, String level) {
        return marketDataService.getMarketDataOrderBook(productId, level);
    }    
  
    public Ticker getMarketDataTicker(String productId) {
        return marketDataService.getMarketDataTicker(productId);
    }     
    
    public List<Trade> getTrades(String productId) {
        return marketDataService.getTrades(productId);
    }
    
    public List<HistoricRate> getMarketDataCandles(String productId) {
        return marketDataService.getMarketDataCandles(productId);
    }     
    
    
	public String helloWorld() {
		return "Hello";
	}
	
}
