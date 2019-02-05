package nu.itark.frosk.crypto.gdax;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;
import nu.itark.frosk.coinbase.exchange.api.marketdata.OrderItem;

//import nu.itark.frosk.coinbase.exchange.api.marketdata.MarketData;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJMarketDataProxy { //extends BaseTest {

 
	
    @Autowired
    MarketDataProxy marketDataProxy;
    
    @Test
    public void testGetMarketDataOrderBook() {
    	MarketData marketData = marketDataProxy.getMarketDataOrderBook("BTC-GBP", "2");
 
    		System.out.println(""+ReflectionToStringBuilder.toString(marketData));
    	
    		List<OrderItem> asks = marketData.getAsks();
    		asks.forEach(ask -> System.out.println("ask="+ReflectionToStringBuilder.toString(ask, ToStringStyle.MULTI_LINE_STYLE)));
    		
    		List<OrderItem> bids = marketData.getBids();
    		bids.forEach(bid -> System.out.println("bid="+ReflectionToStringBuilder.toString(bid, ToStringStyle.MULTI_LINE_STYLE)));
    		
    		Assert.assertTrue(marketData.getSequence() > 0);
    	
    }

    @Test
    public void testHEllok() {
    	
//    	System.out.println("exchange="+exchange);
    	
    	marketDataProxy.helloWorld();
    	
    }
   
    
    
}
