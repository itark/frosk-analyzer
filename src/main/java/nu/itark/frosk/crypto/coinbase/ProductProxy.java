package nu.itark.frosk.crypto.coinbase;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.crypto.coinbase.model.Products;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductProxy {

	@Autowired
    ProductService productService;

    public Candles getCandles(String productId, Instant start, Instant end, Granularity granularity) {
        return productService.getCandles(productId, start, end, granularity);
    }

    public Products getProducts() {
        return productService.getProducts();
    }

    public Product getProduct(String productId) {
        return productService.getProduct(productId);
    }


    //TODO fix below
    public List<Product> getProductsForBaseCurrency(String currency) {
/*
        return getProducts().stream()
                .filter(p -> p.getBase_currency_id().equals(currency))
                .collect(Collectors.toList());
*/
        return null;
    }

    public List<Product> getProductsForQuoteCurrency(String currency) {
/*
        return getProducts().stream()
                .filter(p -> p.getQuote_currency_id().equals(currency))
                .collect(Collectors.toList());
*/
        return null;
    }


}
