package nu.itark.frosk.crypto.coinbase.api.products;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.crypto.coinbase.model.Products;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJProductService {

    @Autowired
    ProductService productService;

    @Autowired
    ProductProxy productProxy;

    @Test
    public final void testProductProxyRaw() {
        Product product = productProxy.getProduct("BTC-EUR");
        System.out.println("product:"+product);
    }

    @Test
    public final void testProducts() {
        Products products = productService.getProducts();
        System.out.println("products:"+products);
    }

    @Test
    public final void testProductsProxy() {
        Products products = productProxy.getProducts();
        System.out.println("products:"+ReflectionToStringBuilder.toString(products));
    }

    @Test
    public final void testGetCandlesOneDayRaw() {
        Instant startTime = Instant.now().minus(400, ChronoUnit.DAYS);
        Instant endTime = Instant.now().minus(100, ChronoUnit.DAYS);
        String candles = productService.getCandlesRaw("BTC-EUR", startTime,endTime, Granularity.ONE_DAY );
        System.out.println("candles:"+candles);;
    }

    @Test
    public final void testGetCandlesOneDay() {
        Instant startTime = Instant.now().minus(400, ChronoUnit.DAYS);
        Instant endTime = Instant.now().minus(100, ChronoUnit.DAYS);
        Candles candles = productService.getCandles("BTC-EUR", startTime,endTime, Granularity.ONE_DAY );
        print(candles);
    }

    @Test
    public final void testGetCandlesFifthMin() {
        Instant startTime = Instant.now().minus(400, ChronoUnit.MINUTES);
        Instant endTime = Instant.now().minus(100, ChronoUnit.MINUTES);
        Candles candles = productService.getCandles("SHPING-EUR", startTime,endTime, Granularity.FIFTEEN_MINUTE );

        print(candles);
    }

    @Test
    public final void testGetCandlesOneDay2() {
        Instant startTime = Instant.now().minus(100, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        Candles candles = productService.getCandles("BTC-EUR", startTime, endTime, Granularity.ONE_DAY);  //WLUNA-USDT7
        print(candles);
    }

    void print(Candles candles) {
        candles.getCandles().forEach(candle -> {
            System.out.println(ReflectionToStringBuilder.toString(candle, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }

}
