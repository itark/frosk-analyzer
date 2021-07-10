package nu.itark.frosk.coinbase.proxy;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.model.Product;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * See class doc for BaseIntegrationTest
 *
 * Created by robevansuk on 14/02/2017.
 */
@ExtendWith(SpringExtension.class)
@Import({IntegrationTestConfiguration.class})
public class MarketDataIntegrationTest extends BaseIntegrationTest {

    ProductService productService;
    MarketDataService testee;

    @BeforeEach
    void setUp() {
        productService = new ProductService(exchange);
        testee = new MarketDataService(exchange);
    }

    @Test
    public void canGetMarketDataForLevelOneBidAndAsk() {
        MarketData marketData = testee.getMarketDataOrderBook("BTC-GBP", 1);
        System.out.println(marketData);
        marketData.getAsks().forEach(o -> {
            System.out.println(ReflectionToStringBuilder.toString(o));
        });
        assertTrue(marketData.getSequence() > 0);
    }

    @Test
    public void canGetMarketDataForLevelTwoBidAndAsk() {
        MarketData marketData = testee.getMarketDataOrderBook("BTC-GBP", 2);
        System.out.println(marketData);
        assertTrue(marketData.getSequence() > 0);
    }

    /**
     * note that the returned results are slightly different for level 3. For level 3 you will see an
     * order Id rather than the count of orders at a certain price.
     */
    @Test
    public void canGetMarketDataForLevelThreeBidAndAsk() {
        MarketData marketData = testee.getMarketDataOrderBook("BTC-GBP", 3);
        System.out.println(marketData);
        assertTrue(marketData.getSequence() > 0);
    }

    @Test
    public void canGetLevel1DataForAllProducts(){
        List<Product> products = productService.getProducts();
        for(Product product : products){
            System.out.print("\nTesting: " + product.getId());
            MarketData data = testee.getMarketDataOrderBook(product.getId(), 1);
            assertNotNull(data);

            if(data.getBids().size() > 0 && data.getAsks().size() > 0) {
                System.out.print(" B: " + data.getBids().get(0).getPrice() + " A: " + data.getAsks().get(0).getPrice());
            } else {
                System.out.print(" NO DATA ");
            }
            try {
               Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
