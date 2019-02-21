package nu.itark.frosk.crypto.coinbase;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.marketdata.HistoricRate;
import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;
import nu.itark.frosk.coinbase.exchange.api.marketdata.OrderItem;
import nu.itark.frosk.coinbase.exchange.api.marketdata.Ticker;
import nu.itark.frosk.coinbase.exchange.api.marketdata.Trade;

//import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJMarketDataProxy { //extends BaseTest {

 
	
    @Autowired
    MarketDataProxy marketDataProxy;
    
    @Test
    public void testGetMarketDataOrderBook() {
    	MarketData marketData = marketDataProxy.getMarketDataOrderBook("BTC-EUR", "2");
 
//    		log.info("marketData="+ReflectionToStringBuilder.toString(marketData));
    	
    		List<OrderItem> asks = marketData.getAsks();
    		asks.forEach(ask -> System.out.println("ask="+ReflectionToStringBuilder.toString(ask, ToStringStyle.MULTI_LINE_STYLE)));
    		
    		List<OrderItem> bids = marketData.getBids();
    		bids.forEach(bid -> System.out.println("bid="+ReflectionToStringBuilder.toString(bid, ToStringStyle.MULTI_LINE_STYLE)));
   
    		log.info("marketData.getSequence()="+marketData.getSequence());
    		
    		log.info("asks="+asks.size());
    		log.info("bids="+asks.size());
    		
    		Assert.assertTrue(marketData.getSequence() > 0);
    	
    }

    @Test
    public void testGetMarketDataTicker() {
    	Ticker ticker = marketDataProxy.getMarketDataTicker("BTC-EUR");
 
    	log.info("ticker="+ReflectionToStringBuilder.toString(ticker));
    	
    }
    
    
    @Test
    public void testGetTrades() {
    	List<Trade> tradeList= marketDataProxy.getTrades("BTC-EUR");
//    	tradeList.forEach(trade -> {
//    		log.info("trade.getTime()"+trade.getTime());
//    	});
    }   
    
    
   @Test
   public final void testGetCandles() {
	   List<HistoricRate> candlesList= marketDataProxy.getMarketDataCandles("BTC-EUR");
//	   HistoricRate candlesList= marketDataProxy.getMarketDataCandles("BTC-EUR");
	   
	   log.info("candlesList="+candlesList);
	   
	   candlesList.forEach(candle -> {
		   log.info(ReflectionToStringBuilder.toString(candle));
		   
	   });
	   
	   
	   
   }
   
    
    @Test
    public void testHEllok() {
    	
//    	System.out.println("exchange="+exchange);
    	
    	marketDataProxy.helloWorld();
    	
    }
   
    
    
}
