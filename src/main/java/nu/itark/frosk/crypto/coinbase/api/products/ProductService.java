package nu.itark.frosk.crypto.coinbase.api.products;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.model.*;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 *     https://docs.cloud.coinbase.com/advanced-trade-api/reference/retailbrokerageapi_getproduct
 */
@Slf4j
public class ProductService {

    public static final String PRODUCTS_ENDPOINT = "/products";

    //For raw tests
    public static final String PRODUCTS_ENDPOINT_LIMIT = "/products?limit=2";

    final Coinbase exchange;

    public ProductService(final Coinbase exchange) {
        this.exchange = exchange;
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

    public Candles getCandles(String productId, Map<String, String> queryParams) {
        StringBuffer url = new StringBuffer(PRODUCTS_ENDPOINT + "/" + productId + "/candles");
        if (queryParams != null && queryParams.size() != 0) {
            url.append("?");
            url.append(queryParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(joining("&")));
        }
        log.info("Retrieving candles for:{} ",productId);
        return exchange.get(url.toString(), new ParameterizedTypeReference<Candles>() {});
    }

    public String getCandlesRaw(String productId, Map<String, String> queryParams) {
        StringBuffer url = new StringBuffer(PRODUCTS_ENDPOINT + "/" + productId + "/candles");
        if (queryParams != null && queryParams.size() != 0) {
            url.append("?");
            url.append(queryParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(joining("&")));
        }
        return exchange.get(url.toString(), new ParameterizedTypeReference<String>() {});
    }


    public Candles getCandles(String productId, Instant startTime, Instant endTime, Granularity granularity) {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("start", String.valueOf(startTime.getEpochSecond()));
        }
        if (endTime != null) {
            queryParams.put("end", String.valueOf(endTime.getEpochSecond()));
        }
        if (granularity != null) {
            queryParams.put("granularity", granularity.toString());
        }
        return getCandles(productId, queryParams);
    }

    public String getCandlesRaw(String productId, Instant startTime, Instant endTime, Granularity granularity) {
        Map<String, String> queryParams = new HashMap<>();
        if (startTime != null) {
            queryParams.put("start", String.valueOf(startTime.getEpochSecond()));
        }
        if (endTime != null) {
            queryParams.put("end", String.valueOf(endTime.getEpochSecond()));
        }
        if (granularity != null) {
            queryParams.put("granularity", granularity.toString());
        }
        return getCandlesRaw(productId, queryParams);
    }


}
