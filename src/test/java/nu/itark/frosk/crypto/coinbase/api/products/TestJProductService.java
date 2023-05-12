package nu.itark.frosk.crypto.coinbase.api.products;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
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
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TestJProductService extends BaseIntegrationTest {

    @Autowired
    ProductService productService;

    @Test
    public final void testProductRaw() {
        String product = productService.getProductRaw("BTC-EUR");
        System.out.println("product:"+product);
    }

    @Test
    public final void testProductsRaw() {
        String products = productService.getProductsRaw();
        System.out.println("products:"+products);
    }

    @Test
    public final void testProducts() {
        Products products = productService.getProducts();
        System.out.println("products:"+ReflectionToStringBuilder.toString(products));
    }

    void print(Candles candles) {
        candles.getCandleList().forEach(candle -> {
            System.out.println(ReflectionToStringBuilder.toString(candle, ToStringStyle.SHORT_PREFIX_STYLE));
        });
    }

}
