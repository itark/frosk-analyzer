package nu.itark.frosk.repo.coinbase;

import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import com.coinbase.exchange.model.Product;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ExtendWith(SpringExtension.class)
@Import({IntegrationTestConfiguration.class})
@SpringBootTest
public class TestJProductProxy extends BaseIntegrationTest {

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
            System.out.println(ReflectionToStringBuilder.toString(candle, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }

}
