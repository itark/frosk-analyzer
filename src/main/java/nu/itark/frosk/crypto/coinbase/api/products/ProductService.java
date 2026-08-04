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

    public static final String PRODUCT_BOOK_ENDPOINT = "/product_book";

    //For raw tests
    public static final String PRODUCTS_ENDPOINT_LIMIT = "/products?limit=2";

    final Coinbase exchange;

    public ProductService(final Coinbase exchange) {
        this.exchange = exchange;
    }

    public Product getProduct(String productId) {
        return exchange.get(PRODUCTS_ENDPOINT + "/" + productId, new ParameterizedTypeReference<Product>() {} );
    }

    /**
     * Top-of-book snapshot for {@code productId}, used to record the bid/ask spread
     * at signal time — the cost that a fill-at-bar-close assumption silently omits.
     *
     * <p>Returns null on any failure. Spread is diagnostic data; it must never fail
     * or delay a trading signal.
     */
    public ProductBook getProductBook(String productId) {
        try {
            return exchange.get(PRODUCT_BOOK_ENDPOINT + "?product_id=" + productId + "&limit=1",
                    new ParameterizedTypeReference<ProductBook>() {});
        } catch (Exception e) {
            log.warn("ProductService: could not read book for {} — {}", productId, e.toString());
            return null;
        }
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

    /**
     * Candles via the PUBLIC market-data endpoint — no authentication, no JWT
     * signing. Same response shape as {@link #getCandles}. Preferred for
     * high-frequency polling (e.g. the 15m crypto intraday sync), since market
     * data does not need a signed request.
     *
     * @see <a href="https://docs.cdp.coinbase.com/advanced-trade/reference/retailbrokerageapi_getpubliccandles">GetPublicCandles</a>
     */
    public Candles getPublicCandles(String productId, Instant startTime, Instant endTime, Granularity granularity) {
        StringBuilder url = new StringBuilder(exchange.getBaseUrl())
                .append("/market").append(PRODUCTS_ENDPOINT).append("/").append(productId).append("/candles?");
        if (startTime != null) {
            url.append("start=").append(startTime.getEpochSecond()).append("&");
        }
        if (endTime != null) {
            url.append("end=").append(endTime.getEpochSecond()).append("&");
        }
        if (granularity != null) {
            url.append("granularity=").append(granularity);
        }
        log.info("Retrieving public candles for:{}", productId);
        return new org.springframework.web.client.RestTemplate()
                .getForObject(url.toString(), Candles.class);
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
