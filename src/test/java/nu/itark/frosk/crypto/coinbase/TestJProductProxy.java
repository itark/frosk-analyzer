package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.marketdata.OrderItem;
import com.coinbase.exchange.api.marketdata.Trade;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import com.coinbase.exchange.model.Product;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.changedetection.LimitOrderImbalance;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import nu.itark.frosk.coinbase.exchange.api.marketdata.Candle;
import nu.itark.frosk.strategies.stats.ADF;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import({IntegrationTestConfiguration.class})
@Slf4j
public class TestJProductProxy extends BaseIntegrationTest {

	static String productId = "BTC-USD";
//	static String LEVEL_1 = "1";
//	static String LEVEL_2 = "2";
	
	/* Fiat EURO
	BTC/EUR
	BCH/EUR
	ETH/EUR
	ETC/EUR
	LTC/EUR
	XLM/EUR
	XRP/EUR
	ZRX/EUR
	*/


    @Autowired
    ProductProxy productProxy;

    @Test
    public final void testBaseCurrencyProducts() {
        List<Product> btcProducts = productProxy.getProductsForBaseCurrency("BTC");
        btcProducts.forEach(b-> {
            System.out.println("id:"+b.getId());
        });
    }

    @Test
    public final void testQuoteCurrencyProducts() {
        List<Product> btcProducts = productProxy.getProductsForQuoteCurrency("EUR");
        btcProducts.forEach(b-> {
            System.out.println("id:"+b.getId());
        });
    }

   @Test
   public final void testGetCandlesOneDay() {
	   Instant startTime = Instant.now().minus(400, ChronoUnit.DAYS);
	   Instant endTime = Instant.now().minus(100, ChronoUnit.DAYS);
	   Candles candles = productProxy.getCandles("BTC-EUR", startTime,endTime, Granularity.ONE_DAY );
       print(candles);
   }

    @Test
    public final void testGetCandlesFifthMin() {
        Instant startTime = Instant.now().minus(400, ChronoUnit.MINUTES);
        Instant endTime = Instant.now().minus(100, ChronoUnit.MINUTES);
        Candles candles = productProxy.getCandles("BTC-EUR", startTime,endTime, Granularity.FIFTEEN_MIN );
        print(candles);
    }

    @Test
    public final void testGetCandlesOneDay2() {
        Instant startTime = Instant.now().minus(100, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        Candles candles = productProxy.getCandles("BTC-EUR", startTime, endTime, Granularity.ONE_DAY);
        print(candles);
    }


    void print(Candles candles) {
        candles.getCandleList().forEach(candle -> {
            log.info(ReflectionToStringBuilder.toString(candle, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }

}
