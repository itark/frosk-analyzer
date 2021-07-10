package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.marketdata.Trade;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProductProxy {

	@Autowired
    ProductService productService;

    public Candles getCandles(String productId, Instant start, Instant end, Granularity granularity) {
        return productService.getCandles(productId, start, end, granularity);
    }

}
