package nu.itark.frosk.crypto.coinbase.api.products;

import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.crypto.coinbase.model.Products;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Created by robevansuk on 03/02/2017.
 */
public class ProductService {

    public static final String PRODUCTS_ENDPOINT = "/products";

    //For tests
    public static final String PRODUCTS_ENDPOINT_LIMIT = "/products?limit=2";

    final Coinbase exchange;

    public ProductService(final Coinbase exchange) {
        this.exchange = exchange;
    }
    //https://docs.cloud.coinbase.com/advanced-trade-api/reference/retailbrokerageapi_getproduct
    public Product getProductORG(String productId) {
        return exchange.get(PRODUCTS_ENDPOINT + "/" + productId, new ParameterizedTypeReference<Product>() {
        });
    }

    public Product getProduct(String productId) {
        return exchange.get(PRODUCTS_ENDPOINT + "/" + productId, new ParameterizedTypeReference<Product>() {} );
    }

    public String getProductRaw(String productId) {
        return exchange.get(PRODUCTS_ENDPOINT_LIMIT + "/" + productId, new ParameterizedTypeReference<String>() {} );
    }

    public Products getProducts() {
        return exchange.get(PRODUCTS_ENDPOINT, new ParameterizedTypeReference<Products>() {});
    }

    public String getProductsRaw() {
        return exchange.get(PRODUCTS_ENDPOINT_LIMIT, new ParameterizedTypeReference<String>() {});
    }

    public Candles getCandles(String productId) {
        return new Candles(exchange.get(PRODUCTS_ENDPOINT + "/" + productId + "/candles", new ParameterizedTypeReference<List<String[]>>() {
        }));
    }

    public Candles getCandles(String productId, Map<String, String> queryParams) {
        StringBuffer url = new StringBuffer(PRODUCTS_ENDPOINT + "/" + productId + "/candles");
        if (queryParams != null && queryParams.size() != 0) {
            url.append("?");
            url.append(queryParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(joining("&")));
        }
        return new Candles(exchange.get(url.toString(), new ParameterizedTypeReference<List<String[]>>() {}));
    }

    /**
     * If either one of the start or end fields are not provided then both fields will be ignored.
     * If a custom time range is not declared then one ending now is selected.
     */
    public Candles getCandles(String productId, Instant startTime, Instant endTime, Granularity granularity) {

        Map<String, String> queryParams = new HashMap<>();

        if (startTime != null) {
            queryParams.put("start", startTime.toString());
        }
        if (endTime != null) {
            queryParams.put("end", endTime.toString());
        }
        if (granularity != null) {
            queryParams.put("granularity", granularity.get());
        }

        return getCandles(productId, queryParams);
    }

    /**
     * The granularity field must be one of the following values: {60, 300, 900, 3600, 21600, 86400}
     */
    public Candles getCandles(String productId, Granularity granularity) {
        return getCandles(productId, null, null, granularity);
    }

    /**
     *  If either one of the start or end fields are not provided then both fields will be ignored.
     */
    public Candles getCandles(String productId, Instant start, Instant end) {
        return getCandles(productId, start, end, null);
    }
}
