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
    
    public List<HistoricRate> getMarketDataCandles(String productId, String start, String end, String granularity) {
        return marketDataService.getMarketDataCandles(productId, start, end, granularity);
    }     
    
    /**
     * The granularity field must be one of the following values: {60, 300, 900, 3600, 21600, 86400}. Otherwise, your request will be rejected. 
     * These values correspond to timeslices representing one minute, five minutes, fifteen minutes, one hour, six hours, and one day, respectively.
     */
	public enum GranularityEnum {
		ONE_MINUTE("60"),
		FIVE_MINUTES("300"),
		FIFTEEN_MINUTES("900"),
		ONE_HOUR("3600"),
		SIX_HOUR("21600"),
		ONE_DAY("86400");

		private String value;

		GranularityEnum(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}
    
	public String helloWorld() {
		return "Hello";
	}
	
}
