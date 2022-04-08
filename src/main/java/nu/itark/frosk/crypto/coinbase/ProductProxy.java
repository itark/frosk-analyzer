package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.marketdata.Trade;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import com.coinbase.exchange.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductProxy {

	@Autowired
    ProductService productService;

    public Candles getCandles(String productId, Instant start, Instant end, Granularity granularity) {
        return productService.getCandles(productId, start, end, granularity);
    }

    public List<Product> getProducts() {
        return productService.getProducts();
    }

    public List<Product> getProductsForBaseCurrency(String currency) {
        return getProducts().stream()
                .filter(p-> p.getBase_currency().equals(currency))
                .collect(Collectors.toList());
    }

    public List<Product> getProductsForQuoteCurrency(String currency) {
        return getProducts().stream()
                .filter(p-> p.getQuote_currency().equals(currency))
                .collect(Collectors.toList());
    }

}
