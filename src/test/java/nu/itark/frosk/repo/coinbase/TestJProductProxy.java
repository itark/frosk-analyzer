package nu.itark.frosk.repo.coinbase;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.crypto.coinbase.model.Products;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TestJProductProxy extends BaseIntegrationTest {

    @Autowired
    ProductProxy productProxy;

    @Test
    public final void testProduct() {
        Product product = productProxy.getProduct("BTC-EUR");
        System.out.println("id:"+product);
    }

    @Test
    public final void testProducts() {
        Products products = productProxy.getProducts();
        System.out.println("id:"+products);
    }

}
