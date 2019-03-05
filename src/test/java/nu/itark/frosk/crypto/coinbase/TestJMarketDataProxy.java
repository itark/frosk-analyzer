package nu.itark.frosk.crypto.coinbase;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import nu.itark.frosk.util.DateTimeManager;

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
	   
	   /*
	   If either one of the start or end fields are not provided then both fields will be ignored. If a custom time range is not declared then one ending now is selected.

	   The granularity field must be one of the following values: {60, 300, 900, 3600, 21600, 86400}. Otherwise, your request will be rejected. 
	   These values correspond to timeslices representing one minute, five minutes, fifteen minutes, one hour, six hours, and one day, respectively.
	   */
	   
	   ZonedDateTime endZdt = ZonedDateTime.now(ZoneId.systemDefault());
	   ZonedDateTime startZdt = endZdt.minusDays(1);
//	   String start = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(startZdt);
//	   String end = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(endZdt);
	
	   String start = DateTimeManager.start(1);
	   String end =  DateTimeManager.end();
	   
//	   List<HistoricRate> candlesList= marketDataProxy.getMarketDataCandles("BTC-EUR", "2019-02-22T00:00:00.00000Z","2019-02-23T00:00:00.00000Z", "3600" );
//	   List<HistoricRate> candlesList= marketDataProxy.getMarketDataCandles("BTC-EUR", start,end, "3600" );
	   List<HistoricRate> candlesList= marketDataProxy.getMarketDataCandles("BTC-EUR", start,end, MarketDataProxy.GranularityEnum.FIFTEEN_MINUTES.getValue() );
	   
	   
	   candlesList.forEach(candle -> {
		   log.info(ReflectionToStringBuilder.toString(candle));
		   
	   });
	   
	   log.info("candlesList.size="+candlesList.size());
	   log.info("start="+start);
	   log.info("end="+end);
	   
	   
   }
   
    
    @Test
    public void testHEllok() {
    	
//    	System.out.println("exchange="+exchange);
    	
    	marketDataProxy.helloWorld();
    	
    }
   
    
    
}
